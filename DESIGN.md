# Decisiones de diseño — Grupo Alan-Kay

## Modelo de dominio

`Expedition` es la entidad central: tiene objetivos, período, zonas de trabajo, responsables, permisos, restricciones, un itinerario (lista ordenada de actividades programadas) y un estado (borrador / revisada / aprobada / en ejecución / suspendida / finalizada). Se apoya en un catálogo de recursos (`ResourceCatalog`: personas, vehículos, instrumentos y consumibles).

Cada ítem del itinerario (`ItineraryItem`) referencia una `Activity` (con sus reglas propias según el tipo — duración, riesgo, recursos y personal requerido) más el horario real y los recursos asignados para esa expedición puntual. Antes de poder aprobarse, una expedición se corre contra un conjunto de reglas de validación (superposiciones, recursos y consumibles faltantes, certificaciones, permisos, capacidad y tolerancia de riesgo); las críticas bloquean la aprobación, y las advertencias se pueden aceptar por un responsable dejando una justificación registrada.

## Patrones y principios aplicados

### D1 — Tipos de actividad: composición en vez de herencia (Strategy)

- **Patrón / principio:** Strategy (Open/Closed).
- **Dónde:** `Activity` (en `business.models.activities`) + interfaz `ActivityRules` (en `business.interfaces.activities`); implementaciones `SampleCollectionRules` y `DivingRules` en `business.rules`.
- **Por qué:** `Activity` es una única clase con los campos comunes obligatorios a todo tipo (nombre, ventana temporal, zona, dependencias), y delega en `ActivityRules` todo lo que varía según el tipo: duración estimada, riesgo, recursos y personal requerido. El enunciado pide que cada tipo tenga sus propias reglas, y agregar un tipo nuevo debe significar agregar una clase, no modificar un `if`/`switch` existente.
- **Alternativas descartadas:** herencia (subclases de `Activity` por tipo) — mezclaría campos comunes con comportamiento variable en la misma jerarquía. `enum` + `switch` por tipo — antipatrón señalado en clase, viola Open/Closed.


### D2 — Validaciones como objetos con severidad (Policy) + política de aprobación

- **Patrón / principio:** Policy (una regla por objeto) + Open/Closed.
- **Dónde:** interfaz `ValidationRule` (en `business.interfaces.validation`); implementaciones en `business.validations`: `ResourceOverlapValidation`, `MissingResourcesValidation`, `InsufficientSuppliesValidation`, `MissingCertificationsValidation`, `MissingPermitsValidation`, `CapacityExceededValidation` y `RiskToleranceValidation`; `ApprovalPolicy` (D31); resultados en `business.models.validation`: `ValidationResult`, `Severity` y `ApprovalResult`.
- **Por qué:**
  - Cada regla es un objeto independiente que devuelve los problemas que encuentra, cada uno con su severidad (crítica o advertencia). La política de aprobación corre todas las reglas y arma un `ApprovalResult` que separa críticas de advertencias y responde `canBeApproved()`, que es exactamente lo que exige el enunciado para decidir si una expedición puede aprobarse.
  - Agregar una regla es agregar una clase y sumarla a la política (como obligatoria, o como adicional al construirla), sin modificar las reglas existentes (Open/Closed). `RiskToleranceValidation` es el ejemplo: no está en la lista del enunciado y se sumó sin tocar las demás.
  - Cada regla devuelve una **lista** de resultados y no uno solo: una misma regla puede encontrar varios problemas (ej. dos actividades sin buzos certificados), y la validación de un borrador tiene que juntarlos todos (D14).
  - La falta de recursos quedó en dos reglas: `MissingResourcesValidation` cuenta personal y equipos de cada ítem, e `InsufficientSuppliesValidation` suma lo que piden todas las actividades que usan un mismo consumible y lo compara con su stock disponible. Son cálculos distintos que cambian por motivos distintos (SRP). Lo que pide el itinerario de cada consumible lo calcula `Itinerary.supplyDemands()`, porque también lo usa la reserva de stock (D30).
  - Severidades elegidas: son críticas las que hacen imposible o no permitido ejecutar el plan (un recurso en dos lugares a la vez, personal, equipos o stock que no alcanzan, personal sin certificación, permisos faltantes, más participantes que los permitidos). Es advertencia una actividad con más riesgo que la tolerancia de la expedición: se puede ejecutar, pero un responsable tiene que aceptarlo (D22).
  - `ValidationResult` es un `record` que dice qué regla lo produjo y sobre qué es (D32): validar dos veces la misma expedición produce el mismo hallazgo, y eso permite reconocer una advertencia aceptada aunque se vuelva a validar.
- **Alternativas descartadas:**
  - Dos interfaces separadas (una para críticas, otra para advertencias) — obligaría a duplicar una regla si su severidad pudiera depender del contexto.
  - Lista de validaciones sueltas sin severidad (boceto inicial) — no permite distinguir qué bloquea la aprobación de qué no.
  - Que cada regla devuelva un `Optional` con un único resultado — se perderían todos los problemas menos el primero.
  - Todos los chequeos en un método de `Expedition` — mezclaría en una clase reglas que cambian por motivos distintos (permisos, stock, certificaciones).


### D3 — Builder para Expedición, con validación de negocio separada

- **Patrón / principio:** Builder + separación de responsabilidades (SRP).
- **Dónde:** `ExpeditionBuilder` + `Expedition` (en `business.models.expeditions`).
- **Por qué:**
  - `Expedition` tiene muchos datos y colecciones (objetivos, zonas, responsables, permisos otorgados, restricciones, actividades), lo que hace ilegible un constructor tradicional. El constructor de `Expedition` es package-private: desde afuera del paquete, la única forma de crear una expedición es el builder.
  - El builder no repite validaciones. `build()` llama al constructor, que aplica los guards de D16 (textos obligatorios, colecciones obligatorias y no vacías), y agenda las actividades con el mismo `Expedition.schedule` que se usa después de crear la expedición. Así las reglas para agendar (D21) viven en un solo lugar, se agende desde el builder o después.
  - La validación de negocio pesada corre aparte con la `ApprovalPolicy` (D2, D31) antes de permitir el paso a "Approved" — así el builder no termina siendo responsable de todo.
- **Alternativas descartadas:**
  - Builder + Director — más estructura de la que hace falta para este caso.
  - Validar todas las reglas de negocio dentro de `build()` — mezclaría "está bien formado" con "es válido para operar", que tienen ciclos de vida distintos.
  - Que el builder reciba `ItineraryItem` ya armados — obligaría a que existan ítems fuera de una expedición y a repetir en el builder los chequeos de zona, período y dependencias.


### D4 — Estimación como agregación, Informe como caso de uso que reutiliza

- **Patrón / principio:** evitar duplicación (DRY) reutilizando Strategy de D1.
- **Dónde:** `ExpeditionEstimator` (en `business.estimations`) y su resultado `ExpeditionEstimate` (en `business.models.estimation`); el informe (`ExpeditionReporter` + `ExpeditionReport`) reutiliza este servicio.
- **Por qué:** la estimación no calcula nada propio: recorre el itinerario y le pregunta a cada actividad, que a su vez delega en su `ActivityRules` (D1), así que agregar un tipo de actividad no toca al estimador. Las tres agregaciones tienen reglas distintas: la duración se suma, el riesgo global es el más alto de sus actividades (`RiskLevel.isAbove`) y el consumo se acumula por categoría sumando `Quantity`, que es la aritmética que ya existe (D8). Estimar no cambia nada ni exige un estado, así que se puede estimar un borrador, que es cuando más sirve para decidir. El equipamiento no se agrega: sumar "1 bote" de tres actividades daría 3 botes cuando puede ser el mismo reutilizado, y lo que hace falta por actividad ya lo verifica `MissingResourcesValidation` (D2).
- **Alternativas descartadas:** calcular el resumen de riesgos del informe con lógica propia, independiente de `ExpeditionEstimator` — duplicaría reglas de cálculo. Builder para `ExpeditionEstimate` — no tiene invariantes que proteger, es una proyección de datos. Que la duración total sea el lapso entre el primer inicio y el último fin del itinerario — eso mide cuánto dura el plan en el calendario y no cuánto trabajo hay, porque los huecos entre actividades no son esfuerzo estimado.


### D5 — Actividad vs. su programación dentro de la expedición

- **Patrón / principio:** separación entre lo que se planifica y cómo se programa (reificación del ítem de itinerario).
- **Dónde:** `Activity` (qué hay que hacer, en qué zona, dentro de qué ventana temporal, después de qué actividades y con qué `ActivityRules`) e `ItineraryItem` (el horario concreto elegido dentro de esa ventana y los recursos concretos asignados).
- **Por qué:**
  - La ventana temporal de `Activity` es una **restricción** ("el muestreo se hace entre las 8 y las 18"); el `scheduledPeriod` del ítem es una **decisión** tomada dentro de esa restricción ("de 9 a 11"). Separarlos permite controlar una contra la otra: un ítem no puede quedar fuera de la ventana de su actividad.
  - Las personas, equipos y consumibles asignados cambian mientras se planifica y se replanifica, pero la definición de la actividad no. Por eso `Activity` sigue siendo inmutable y lo que cambia vive en el ítem.
- **Alternativas descartadas:**
  - Una sola clase `Activity` con horario y recursos asignados — tendría campos vacíos mientras la actividad no está programada y volvería mutable algo que hoy es inmutable.
  - Modelar `Activity` como catálogo reutilizable entre expediciones, moviendo zona, ventana y dependencias al ítem — esos datos solo tienen sentido dentro de una expedición concreta (una dependencia apunta a otra actividad de la misma expedición), el enunciado no pide reutilizar actividades entre expediciones y obligaría a rehacer D1 y D11.


### D6 — El itinerario como la colección ordenada de actividades de una expedición

- **Patrón / principio:** encapsulamiento de colección.
- **Dónde:** `Itinerary` (uno por `Expedition`), que crea y guarda los `ItineraryItem`.
- **Por qué:**
  - Cada expedición tiene su propio itinerario, que es el conjunto de ítems (D5) programados para esa expedición. Es a través del itinerario que se accede a las actividades de una expedición — no hay una lista de actividades suelta aparte. Esto mantiene el orden y las dependencias en un solo lugar, y es lo que recorren tanto las validaciones (D2) como la estimación (D4).
  - El orden no se carga a mano: el itinerario mantiene los ítems ordenados por hora de inicio, que es el orden en que se ejecutan.
  - `getItems()` devuelve una vista no modificable; agregar ítems es package-private y solo lo hace `Expedition` (D19).
- **Alternativas descartadas:**
  - Que `Expedition` tenga una lista de actividades sin un itinerario que las agrupe — perdería el lugar natural para el orden, las dependencias entre actividades programadas y los datos propios de cada ítem.
  - Orden manual por posición (mover arriba o abajo) — permitiría un orden que contradice los horarios.


### D7 — Dos contratos de recurso: por tiempo y por stock

- **Patrón / principio:** Interface Segregation (ISP).
- **Dónde:** `business.interfaces.resources`: `Resource` (padre, solo `getId()`), `ReusableResource` (`Person`, `Vehicle`, `Instrument`) y `DepletableResource` (`Depletable`). `Vehicle` e `Instrument` llegan a `ReusableResource` a través de `Equipment` (D17). Las implementaciones viven en `business.models.resources`.
- **Por qué:** un recurso reutilizable se reserva por franjas horarias (`isAvailableDuring`, `reserve`) y uno consumible se agota (`hasStockFor`, `consume`). Son preguntas distintas que hacen clientes distintos, así que cada una tiene su interfaz chica. `Resource` es el tipo común para cuando alcanza con identificar el recurso (ej. los recursos asignados a un ítem del itinerario). No se usa con `instanceof` para decidir cómo tratar cada tipo: para eso están las interfaces hijas.
- **Alternativas descartadas:** una única interfaz `Resource` con los cuatro métodos, que obligaría a implementar métodos sin sentido (un combustible no se "reserva" por horario; una persona no tiene stock). Converger después, si la cátedra confirma que es un único concepto, rompe menos código que separar una interfaz ya unificada.


### D8 — Reificación de conceptos del dominio en value objects

- **Patrón / principio:** value objects.
- **Dónde:** `TimePeriod`, `Quantity` + `MeasurementUnit`, `Certification`, `Permit`, `ExpeditionRestrictions`.
- **Por qué:** cada uno encapsula su regla en un solo lugar. `TimePeriod` resuelve la superposición como intervalo semiabierto `[inicio, fin)`, así que dos franjas que solo se tocan en el borde no chocan. `Quantity` usa `BigDecimal`, no admite negativos y no permite operar con unidades distintas. `Certification` y `Permit` evitan comparar `String` sueltos. Son `record` inmutables con igualdad por valor.
- **Restricciones generales:** `ExpeditionRestrictions` agrupa las restricciones de la expedición (máximo de participantes y tolerancia de riesgo) en un `record` en vez de una lista de `String`: la validación (D2) tiene que comparar contra ellas, y un texto libre no se puede comparar.
- **Detalle de implementación:** `Quantity` normaliza el monto con `stripTrailingZeros()` en su constructor compacto, porque `BigDecimal.equals` compara también la escala (`20` no sería igual a `20.0`) y eso rompería la igualdad por valor que da el `record`. Sin esa normalización, dos cantidades que representan lo mismo no serían intercambiables como claves ni en asserts. La contracara es que `stripTrailingZeros()` escribe `100` como `1E+2`, así que `Quantity` define su propio `toString()` con `toPlainString()`: los mensajes de error dicen `100 LITERS` y no `1E+2 LITERS`.
- **Alternativas descartadas:** `LocalDateTime` sueltos, `double` + `String` de unidad y listas de `String` para certificaciones y permisos. Esto repartiría la lógica de solapamiento y de unidades en cada clase que la use. Comparar con `compareTo` en vez de normalizar la escala: obligaría a redefinir `equals`/`hashCode` a mano y perdería la ventaja de usar `record`.


### D9 — Disponibilidad temporal compartida: la regla por composición, la agenda en una clase base

- **Patrón / principio:** composición sobre herencia para la regla, sin duplicación (DRY).
- **Dónde:** `AvailabilityCalendar` tiene la regla; `ScheduledResource` (abstracta) implementa `ReusableResource` con un calendario: tiene las operaciones de agenda (`isAvailableDuring`, `reserve`, `releaseAll`, `markUnavailable`) y deja `getId` a cada clase. `Person`, `Vehicle` e `Instrument` la extienden, suman sus otros contratos (`Equipment`, `Certifiable`) y conservan lo propio (id, nombre, certificaciones, patente, categoría).
- **Por qué:**
  - Los tres recursos reutilizables comparten exactamente la misma regla de disponibilidad (no reservar franjas superpuestas). Esa regla vive en `AvailabilityCalendar` y cambiarla es tocar una sola clase. El calendario guarda reservas con dueño (`Reservation`, D27), no franjas sueltas.
  - **Revisado:** en la primera versión cada recurso delegaba por su cuenta en su calendario, y se descartó una clase base. Al sumar `releaseAll` y `markUnavailable` (D27), cada operación nueva obligó a escribir la misma delegación en las tres clases. `ScheduledResource` junta solo ese cableado de agenda: una operación nueva de disponibilidad se agrega en un solo lugar, y el calendario sigue siendo privado.
  - El nombre dice lo que es: un recurso reutilizable cuya agenda la lleva un calendario. Por eso implementa `ReusableResource` (y con ella `Resource`), y no tiene nada que no sea agenda: la identidad y las certificaciones quedan en cada clase. Es el mismo esquema que `List` + `AbstractList` en la biblioteca de Java: la interfaz es el contrato que usan los clientes y la clase abstracta, la implementación compartida.
  - La preocupación original (atar la jerarquía a un único eje) queda cubierta porque todos los clientes (catálogo, `ResourceReservations`, validaciones) dependen de la interfaz `ReusableResource`, no de la base. Un recurso con otra forma de disponibilidad implementa la interfaz directamente.
- **Alternativas descartadas:**
  - Copiar la delegación de agenda en cada clase (la versión anterior): cada operación nueva toca todos los recursos.
  - Métodos `default` en `ReusableResource` sobre un `calendar()` público: ahorra lo mismo, pero expone el calendario a cualquier cliente (D15).
  - Poner también id, nombre y certificaciones en la base: mezclaría en una clase de agenda cosas que no son agenda.
  - Una base que no implementa `ReusableResource`: se llamaría "recurso" sin serlo, y cada clase tendría que repetir la declaración del contrato.
  - Una interfaz nueva solo para la agenda: repetiría las cuatro operaciones que ya define `ReusableResource`.


### D10 — Certificaciones como capacidad aparte de la disponibilidad

- **Patrón / principio:** Interface Segregation (ISP) + composición, mismo criterio que D9.
- **Dónde:** interfaz `Certifiable` y value object `Certifications`, usados por `Person`, `Vehicle` e `Instrument`.
- **Por qué:** personas, vehículos e instrumentos pueden tener certificaciones. `Certifiable` permite que la validación de certificaciones faltantes pregunte `hasCertification` sin conocer la clase concreta ni hacer `instanceof`. La regla vive en `Certifications`, así que si una certificación gana reglas propias (ej. vencimiento) se cambia en un solo lugar.
- **Alternativas descartadas:** agregar `hasCertification` a `ReusableResource`, que mezclaría dos preguntas distintas (disponibilidad y habilitación) y obligaría a cualquier recurso reutilizable futuro a tener certificaciones. Repetir un `Set<Certification>` con su lógica en cada clase quedó descartado por duplicación.


### D11 — Los datos propios de cada tipo de actividad viven en su regla; requisitos como value objects

- **Patrón / principio:** Strategy (D1) + value objects (D8).
- **Dónde:** `ActivityRules`, `SampleCollectionRules(sampleCount)`, `DivingRules(maxDepthMeters)`, `StaffRequirement`, `RiskLevel`.
- **Por qué:**
  - De un tipo de actividad a otro no cambian solo las fórmulas, también los datos que usan (cantidad de muestras, profundidad). Cada implementación recibe sus datos en el constructor, así `Activity` tiene solo los campos comunes y los métodos de `ActivityRules` no necesitan parámetros.
  - Los requisitos de recursos se explican en D12.
  - `StaffRequirement` decide si un candidato califica a través de `Certifiable` (D10), sin conocer la clase concreta.
- **Alternativas descartadas:**
  - Pasar la `Activity` a cada método (`estimateDuration(Activity)`): hoy ninguna regla usa datos de la actividad, y además crea una dependencia circular entre `Activity` y sus reglas. Consecuencia: si una regla llega a necesitar un dato común (ej. la zona), hay que agregar el parámetro en todas las implementaciones.
  - Campos específicos de un tipo en `Activity` (ej. `depth` nullable): mezclaría datos de tipos distintos en una sola clase.


### D12 — Requisitos de recursos separados por forma de uso, en espejo con los recursos

- **Patrón / principio:** SRP + value objects, sobre el mismo eje que D7.
- **Dónde:** `ResourceCategory`, `ReusableRequirement(category, count)` y `DepletableRequirement(category, quantity)`; `ActivityRules.requiredEquipment()` y `requiredSupplies()`.
- **Por qué:**
  - Una regla de actividad describe **qué necesita** ("1 bote", "20 litros de combustible"), no qué instancia concreta usar. Elegir la instancia es trabajo de la asignación y de la replanificación.
  - Un recurso responde dos preguntas: **qué es** (`ResourceCategory`: bote, sonar, combustible) y **cómo se usa** (las interfaces de D7: se reserva o se gasta).
  - Los requisitos siguen esa misma división. Uno reutilizable se cuenta en unidades enteras y se cumple con recursos libres en el horario. Uno consumible se mide con `Quantity` y se cumple con stock. Son reglas distintas que cambian por motivos distintos, así que no es duplicación.
- **Alternativas descartadas:**
  - Un único `ResourceRequirement(tipo, Quantity)`: permite pedir "1.5 botes" y obliga a cada cliente a preguntar qué clase de requisito es (`if`/`instanceof` repetido en validación, asignación y estimación).
  - Requisitos que apuntan a instancias concretas (el bote `v-1`): atan la regla a un catálogo y dejan sin sentido la asignación y la replanificación.
  - Interfaz padre `Requirement`: hoy nadie necesita una lista mezclada. Se agrega si aparece ese caso.
- **Resuelto en D17 y D18:** los recursos del catálogo declaran su `ResourceCategory` a través de `Categorized`, y la comparación contra los requisitos vive en los propios requisitos.

### D13 — La zona es dueña de la comparación de permisos

- **Patrón / principio:** Information Expert (la lógica vive donde están los datos).
- **Dónde:** `Zone.missingPermits(grantedPermits)` y `Zone.isAccessibleWith(grantedPermits)`.
- **Por qué:** la `Zone` es la que sabe qué permisos exige, así que es la que compara contra los que tiene la expedición. La validación de permisos faltantes (D2) no necesita leer `requiredPermits` ni saber cómo se comparan: le pregunta a la zona. `missingPermits` devuelve **cuáles** faltan y no solo un booleano, porque el resultado de la validación tiene que poder decir qué permiso falta; `isAccessibleWith` es el atajo para cuando eso no interesa.
- **Alternativas descartadas:** exponer `requiredPermits` con un getter y comparar afuera — repartiría la comparación entre la validación, la asignación y el informe. Devolver solo `boolean` — obligaría a recalcular la diferencia en el lugar donde se arma el mensaje de error.


### D14 — Las preguntas devuelven `boolean`, los comandos fallan con excepción

- **Patrón / principio:** Command-Query Separation + fail fast.
- **Dónde:** `ReusableResource.isAvailableDuring` / `reserve`, `DepletableResource.hasStockFor` / `consume`; `ResourceUnavailableException` e `InsufficientStockException` en `business.exceptions`.
- **Por qué:** cada contrato de recurso (D7) tiene una consulta y un comando. La consulta responde `boolean` porque es exactamente lo que las validaciones (D2) necesitan para decidir sin romper nada: corren sobre una expedición en borrador y tienen que poder juntar **todos** los problemas, no frenarse en el primero. El comando, en cambio, no tiene forma válida de "fallar a medias": si `reserve` o `consume` se ejecutan sobre un recurso que no da, es un error de programación (alguien no consultó antes), y devolver `false` lo dejaría pasar silenciosamente. Por eso son excepciones *unchecked*: no son casos de negocio esperados que el llamador deba manejar, son invariantes rotos. `Depletable.consume` verifica antes de modificar, así que el stock queda intacto cuando falla.
- **Límite conocido:** `hasStockFor` sí lanza `IllegalArgumentException` si la unidad no coincide, porque `Quantity` no opera entre unidades distintas (D8). No es una excepción a la regla de arriba: "no alcanza el stock" es una respuesta válida (`false`), pero "¿me alcanzan 20 litros para 1 kilogramo?" es una pregunta mal formada, no un problema de la expedición. La validación de recursos faltantes (D2) tiene que comparar contra requisitos de la misma unidad; si llega a necesitar tolerar el desajuste, va a hacer falta un `UnitConverter` (ver la tabla de patrones no aplicados).
- **Alternativas descartadas:** comandos que devuelven `boolean` — el `if` de chequeo es fácil de omitir y el error aparece mucho después. Excepciones *checked* — obligarían a un `try/catch` en cada llamador que ya consultó y sabe que va a andar. Devolver `Optional`/`Result` — suma ceremonia sin agregar información: acá no hay más de un motivo de falla por operación.


### D15 — Entidades por identidad (clases), value objects por valor (`record`)

- **Patrón / principio:** distinción entidad / value object.
- **Dónde:** clases con identidad: `Person`, `Vehicle`, `Instrument`, `Depletable`, `Zone`, `Activity`, `Expedition`, `ItineraryItem`. `record` con igualdad por valor: `ExpeditionId`, `ActivityId`, `ResourceId`, `Reservation`, `Unavailability`, `TimePeriod`, `Quantity`, `Certification`, `Certifications`, `Permit`, `ResourceCategory`, `ReusableRequirement`, `DepletableRequirement`, `StaffRequirement`, `ExpeditionRestrictions`, `ValidationResult`, `RuleId`, `ValidationSubject`, `ApprovalResult`, `AcceptedWarning`.
- **Por qué:**
  - Las entidades tienen `id` pero **no** redefinen `equals`/`hashCode`: se comparan por identidad de instancia. Dos `Vehicle` con la misma patente son dos vehículos distintos del catálogo, y cada uno tiene su propio `AvailabilityCalendar` (D9) — igualarlos por `id` haría que reservar uno pareciera reservar al otro.
  - De ahí se apoya `Activity.dependsOn`: una dependencia apunta a *esa* actividad concreta, no a "cualquier actividad que se llame igual".
  - Los value objects son `record`: inmutables, sin identidad propia, intercambiables si valen lo mismo (D8).
- **Convención de accessors:** los `record` exponen sus componentes con el nombre del campo (`name()`, `amount()`), que es lo que genera el lenguaje. Las entidades exponen `getX()` generado por Lombok `@Getter`, para no escribir boilerplate a mano. Son dos estilos porque son dos categorías de objeto distintas, no un descuido.
- **`@Getter` va por campo, nunca sobre la clase:** anotar la clase expone *todo* el estado interno, y varias entidades tienen colaboradores que no deben salir. Queda privado lo que ya tiene un método que responde la pregunta: `requiredPermits` en `Zone` (se pregunta con `missingPermits`, D13), `calendar` en `ScheduledResource` (se pregunta con `isAvailableDuring`, D9), `certifications` en `Person`/`Vehicle`/`Instrument` (se pregunta con `hasCertification`, D10) y `rules` en `Activity` (se delega con `estimatedDuration` y compañía, D1). Exponerlos dejaría que un cliente se saltee la abstracción: si `Activity` devolviera su `ActivityRules`, el Strategy de D1 pasaría a ser público y cualquiera podría puentear la delegación.
- **Caso aparte — `Depletable.getStock()`:** este sí se expone. `stock` es un campo mutable (se reasigna al consumir) pero el `Quantity` que devuelve es inmutable (D8), así que entrega una foto del estado actual y no una referencia con la que romper invariantes. El informe de expedición (D4) necesita leer el consumo real, y no hay forma de obtenerlo preguntando `hasStockFor`.
- **Nota de build:** desde JDK 23 `javac` no corre los annotation processors que encuentra en el classpath. Lombok tiene que declararse explícitamente en el `<annotationProcessorPaths>` del `maven-compiler-plugin`; si falta, `@Getter` no genera nada y el proyecto no compila. El plugin ya activa el procesamiento por su cuenta al ver esa lista, así que no hace falta agregar `<proc>full</proc>`.
- **Alternativas descartadas:** `equals`/`hashCode` por `id` en las entidades — dos instancias con el mismo `id` pero distinto estado (distintas reservas, distinto stock) se tratarían como la misma. Hacer `record` todas las clases — los recursos son mutables por definición (se reservan, se consumen) y `Activity` referencia otras `Activity`, donde la identidad es justamente lo que importa. El caso menos obvio es `Zone`, que es inmutable y técnicamente podría ser un `record`: se deja como entidad porque pertenece a un catálogo y las expediciones la referencian por identidad — dos zonas con el mismo nombre son dos zonas distintas, no la misma.


### D16 — Guards de argumentos compartidos, en un solo lugar

- **Patrón / principio:** fail fast + DRY.
- **Dónde:** `DomainArguments` (`requireText`, `requireSet`, `requireList`, `requireNotEmpty`), usado por `Person`, `Vehicle`, `Instrument`, `Depletable`, `Zone`, `Activity`, `Expedition`, `Certification`, `Certifications`, `Permit`, `ResourceCategory`, `StaffRequirement`, `ExpeditionId`, `ActivityId` y `ResourceId`.
- **Por qué:** antes cada clase validaba distinto. Cuatro rechazaban un `id` en blanco y cinco lo aceptaban, así que `new Person("", "Ana", Set.of())` construía una persona sin identidad que recién iba a romper mucho después. Y las colecciones nulas fallaban dentro de `Set.copyOf`, con el mensaje interno del JDK (`Cannot invoke "java.util.Collection.isEmpty()" because "coll" is null`) en vez de decir qué campo faltaba. Ahora la regla vive en una sola clase: un texto obligatorio no puede ser nulo ni estar en blanco, y una colección obligatoria no puede ser nula y siempre se copia a una versión inmutable.
- **Sobre los tipos de excepción:** un texto mal formado es `IllegalArgumentException` (el argumento llegó, pero no sirve); una colaboración ausente es `NullPointerException` con el nombre del campo, que es la semántica de `Objects.requireNonNull` y la que ya usaban `Activity` y `Depletable` para sus colaboradores. Se mantuvo esa división en vez de unificar todo en una sola, para no cambiar el comportamiento ya testeado.
- **Alternativas descartadas:**
  - Repetir el guard privado que tenía `Activity` en cada clase: el mismo bloque duplicado en nueve lugares, y la garantía de que la próxima clase se olvide de alguna rama.
  - Reificar el identificador en un value object (`ResourceId`), que es lo que hace D8 con `Certification` y `Permit`: es el camino más OO y queda abierto, pero hoy un `id` no tiene ninguna regla propia más allá de "no está vacío" — sería un tipo nuevo por cada entidad sin comportamiento que justifique el costo de tocar todas las firmas. Se reconsidera si los ids ganan reglas (formato, unicidad dentro del catálogo). Aplicado para la expedición (D26) y para actividades y recursos (D32).
- **Consecuencia:** `DomainArguments` es una clase de métodos estáticos, que no es orientada a objetos. Se acepta porque es exactamente el rol de `Objects.requireNonNull` en la biblioteca estándar: una precondición, no una responsabilidad del dominio. Si crece más allá de guards de argumentos, es señal de que hay un concepto sin reificar.


### D17 — La categoría como capacidad; el requisito decide si un recurso lo cumple

- **Patrón / principio:** Interface Segregation (ISP) + Information Expert, mismo criterio que D10 y D13.
- **Dónde:** interfaz `Categorized` (`belongsTo`); `Equipment extends ReusableResource, Categorized` (implementada por `Vehicle` e `Instrument`); `DepletableResource extends Resource, Categorized` (`Depletable`). La comparación vive en `ReusableRequirement.accepts`, `DepletableRequirement.accepts` y `DepletableRequirement.isCoveredBy`.
- **Por qué:**
  - Cierra el pendiente de D12: para saber si un recurso del catálogo cumple un requisito, el recurso tiene que declarar qué es.
  - El requisito es el que conoce la categoría y la cantidad pedidas, así que es el que decide si un candidato lo cumple. El recurso solo responde `belongsTo`, sin exponer su categoría. Validación, asignación y replanificación le preguntan al requisito, y la regla de "cumple" queda en un solo lugar.
  - `Person` no implementa `Categorized`: el personal se pide por certificaciones (`StaffRequirement`, D10 y D11). Darle una categoría sería agregar un campo que nadie consulta.
  - `Equipment` existe porque el catálogo y la asignación necesitan un tipo que sea a la vez "se reserva por horario" y "se pide por categoría". Sin él, el catálogo tendría una lista de `Vehicle` y otra de `Instrument`, y sumar un tipo de equipo nuevo (ej. un dron) obligaría a modificarlo (Open/Closed).
  - `DepletableResource` extiende `Categorized` directamente porque todo consumible se pide por categoría: una interfaz de rol aparte no tendría un cliente distinto que la justifique.
- **Alternativas descartadas:**
  - Exponer `getCategory()` y comparar afuera: la comparación se repetiría en el catálogo, la validación y la asignación.
  - Poner la categoría en `Resource`: obligaría a `Person` a tener una.
  - Resolver el matching dentro del catálogo: el catálogo tendría que conocer la regla de cada tipo de requisito; si esa regla cambia (ej. categorías equivalentes), habría que tocar el catálogo y no el requisito.
- **Consecuencia:** si un requisito de consumible y un recurso de su misma categoría usan unidades distintas, `isCoveredBy` falla con excepción. Es el límite ya documentado en D14: indica un catálogo mal cargado, no un faltante de la expedición.


### D18 — Catálogo de recursos: un recurso por id, consultas por requisito

- **Patrón / principio:** encapsulamiento de colección + invariante de unicidad.
- **Dónde:** `ResourceCatalog` (`addStaff`, `addEquipment`, `addSupply`, `availableStaffFor`, `availableEquipmentFor`, `suppliesCovering`) y `DuplicateResourceException`.
- **Por qué:**
  - El enunciado nombra los "recursos duplicados" como uno de los problemas a resolver. El catálogo rechaza un segundo recurso con un `ResourceId` ya registrado (D32), con un único espacio de ids para todos los tipos: una asignación o un informe que nombra un `id` nunca es ambiguo.
  - Es lo que hace seguro comparar entidades por instancia (D15): con una sola instancia por `id`, la identidad de instancia coincide con la identidad de negocio y no puede haber dos calendarios para el mismo recurso real.
  - Las consultas devuelven **candidatos**, no asignaciones: el catálogo responde "quién puede" (cumple el requisito y está libre, o tiene stock); "quién va" es trabajo de la asignación. Si cambia el criterio para elegir entre candidatos, el catálogo no cambia.
  - Mismo criterio que D14: registrar es un comando y falla con excepción; consultar devuelve una lista, posiblemente vacía.
- **Alternativas descartadas:**
  - Detectar duplicados recién en la validación de la expedición: llega tarde, el mismo recurso real ya pudo reservarse en dos instancias distintas.
  - Un catálogo por tipo de recurso: repetiría la regla de unicidad y permitiría repetir un `id` entre tipos.
  - Exponer las listas con getters y filtrar afuera: cada cliente reimplementaría "cumple el requisito y está libre".
- **Consecuencia:** es una clase concreta en memoria. Si más adelante hay persistencia, el contrato se extrae a `business/providers` con estas mismas consultas y esta clase pasa a ser un detalle.


### D19 — La expedición es la única puerta para modificar su itinerario

- **Patrón / principio:** encapsulamiento (la expedición como raíz de lo que contiene) + fail fast.
- **Dónde:** `Expedition.schedule`, `assignStaff`, `assignEquipment` y `assignSupply`; `Itinerary.add` y los `assign...` de `ItineraryItem` son package-private; `ExpeditionNotEditableException`.
- **Por qué:**
  - Una expedición en revisión o aprobada no puede cambiar su itinerario: lo que se revisó o aprobó tiene que ser lo que se ejecuta. Esa regla depende del estado de la expedición, así que el cambio tiene que pasar por la expedición. Si `ItineraryItem` expusiera `assignStaff` en público, cualquier cliente podría saltearse el chequeo.
  - Por eso lo que modifica `Itinerary` e `ItineraryItem` es package-private: desde afuera del paquete los ítems se leen, pero solo se modifican a través de `Expedition`, que antes verifica el estado y que el ítem sea suyo.
  - Las asignaciones de un ítem son conjuntos: asignar dos veces el mismo recurso al mismo ítem no tiene efecto. Repetir una asignación no es un error de negocio, y así quien propone asignaciones no necesita consultar antes.
- **Alternativas descartadas:**
  - Métodos públicos en `ItineraryItem` y que cada cliente chequee el estado — la regla quedaría repartida y sería fácil de olvidar.
  - Devolver copias de los ítems para que no se puedan modificar — los ítems tienen identidad (D15) y validaciones, asignación e informes necesitan referenciar exactamente el ítem programado.
- **Consecuencia:** `Expedition` concentra varios métodos cortos que verifican y delegan en el ítem. Se acepta porque cada uno es una línea más un chequeo compartido; si con el seguimiento y la replanificación crece demasiado, se evalúa separar responsabilidades.


### D20 — Estados de la expedición con tabla de transiciones en el enum

- **Patrón / principio:** encapsulamiento de la regla en el tipo, sin patrón State (ver tabla de patrones no aplicados).
- **Dónde:** `ExpeditionStatus.canTransitionTo`, `Expedition.submitForReview` y `returnToDraft`; `InvalidStatusTransitionException`.
- **Por qué:**
  - El enunciado define seis estados. Si el enum solo los listara, nada impediría pasar de borrador a en ejecución sin revisión ni aprobación. Las transiciones válidas son una tabla dentro del propio enum, así hay un único lugar donde leer el flujo completo:
    - borrador → revisión
    - revisión → borrador (se devuelve para corregir) o aprobada
    - aprobada → en ejecución
    - en ejecución → suspendida o finalizada
    - suspendida → en ejecución o finalizada
    - finalizada → ninguna
  - `Expedition` le pregunta al enum antes de cada cambio y falla con excepción si la transición no es válida: cambiar de estado es un comando (D14). Terminar exige además que no quede ninguna actividad en curso (D29).
  - Una expedición aprobada no vuelve a borrador: si hay que cambiarla, se construye una alternativa (replanificación).
- **Alternativas descartadas:** un `switch` sobre el estado en cada método de `Expedition` — repetiría el flujo en varios lugares y cada estado nuevo obligaría a tocarlos todos. Una clase por estado — ver la tabla de patrones no aplicados.


### D21 — Qué se rechaza al agendar y qué detecta la validación

- **Patrón / principio:** fail fast para planes mal formados + separación entre "está bien formado" y "es válido para operar" (mismo criterio que D3).
- **Dónde:** `Expedition.schedule`, `Itinerary.add` e `ItineraryItem` lanzan `InvalidScheduleException`; `TimePeriod.contains`.
- **Por qué:**
  - Al agendar se rechaza lo que no tiene sentido como plan, sin importar qué recursos haya: una actividad en una zona que no es de la expedición, un horario fuera del período de la expedición o fuera de la ventana de la actividad, la misma actividad dos veces, o una actividad cuya dependencia no está agendada o todavía no terminó. Son errores de quien arma el itinerario y conviene enterarse en el momento.
  - En cambio, la superposición de recursos, la falta de recursos, las certificaciones, los permisos y la capacidad dependen de asignaciones y del catálogo, que cambian mientras se planifica. Esos problemas se juntan todos con la validación (D2) en vez de frenar en el primero, porque un borrador puede estar incompleto a propósito.
  - Exigir que las dependencias ya estén agendadas obliga a armar el itinerario en orden, y a cambio garantiza que ninguna actividad queda antes de algo que necesita.
  - La contención de períodos vive en `TimePeriod.contains`, junto a `overlapsWith` y con el mismo intervalo semiabierto de D8: una actividad puede empezar justo cuando termina su dependencia.
- **Alternativas descartadas:**
  - Convertir todo en reglas de validación — permitiría armar itinerarios imposibles (una actividad antes de su dependencia) que recién se detectarían al pedir la aprobación.
  - Rechazar al agendar también la falta de recursos — impediría armar el itinerario antes de asignar.


### D22 — La aprobación corre la validación y registra las advertencias aceptadas

- **Patrón / principio:** Dependency Inversion + fail fast.
- **Dónde:** `Expedition.approve(ExpeditionValidator, List<AcceptedWarning>)`; interfaz `ExpeditionValidator` (en `business.interfaces.validation`), implementada por `ApprovalPolicy` (D31); `AcceptedWarning`; `ExpeditionNotApprovableException` y `UnacceptedWarningException`.
- **Por qué:**
  - El enunciado exige que una expedición con errores críticos no pueda pasar a aprobada. Para que eso no dependa de que alguien se acuerde de validar antes, `approve` recibe el validador y lo corre en ese momento: no se puede aprobar con un resultado viejo o armado a mano.
  - Cada advertencia tiene que estar cubierta por un `AcceptedWarning` de alguien que sea responsable de la expedición, con una justificación no vacía. Las aceptaciones quedan registradas en la expedición (`getAcceptedWarnings`) para el informe. No se puede crear un `AcceptedWarning` sobre un resultado crítico: lo crítico no se acepta, se corrige. Una aceptación cubre una advertencia si es el mismo hallazgo (misma regla y mismo sujeto), no si el texto coincide (D32).
  - `Expedition` depende de la interfaz `ExpeditionValidator` y no de la política concreta, igual que `Activity` depende de `ActivityRules` (D1). Así los tests de la entidad usan un validador de prueba con resultados fijos, y la política y cada regla se prueban por separado. Qué reglas son obligatorias lo garantiza el caso de uso de aprobación con la `ApprovalPolicy` (D31).
  - La transición se verifica antes de validar: una expedición que no está en revisión ni siquiera se valida.
  - Al aprobar se reservan los recursos reutilizables asignados, cada uno para el horario de su ítem. Recién ahí el plan compromete recursos: mientras es borrador se puede asignar y reasignar libremente, y las demás expediciones ven esos recursos ocupados recién cuando esta se aprueba. La reserva no la hace la expedición: la coordina el caso de uso `ApproveExpedition` (D28) con `ResourceReservations`, todo o nada (D27).
- **Alternativas descartadas:**
  - `approve(ApprovalResult)` — permitiría aprobar con un resultado calculado antes de un cambio, o con uno vacío.
  - Que `Expedition` dependa directamente de `ApprovalPolicy` — ataría la entidad a una implementación y a su lista concreta de reglas (ver D31).
  - `Map<ValidationResult, String>` de justificaciones (boceto inicial) — no registra quién aceptó cada advertencia, y el enunciado pide que la acepte un responsable.
  - Reservar los recursos al asignarlos — la validación de superposición nunca encontraría nada, porque la segunda reserva fallaría con excepción en vez de reportarse junto con los demás problemas.
- **Consecuencia:** si algún recurso no se puede reservar (tomado por otra expedición entre la validación y la aprobación, o compartido por dos ítems propios en horarios superpuestos), `ApproveExpedition` corta con `ResourceUnavailableException` sin aprobar, sin reservar nada y sin registrar advertencias (D27, D28).


### D23 — El seguimiento es el estado real de cada ítem, y entra por la expedición

- **Patrón / principio:** encapsulamiento (misma puerta única que D19) + Information Expert.
- **Dónde:** `ActivityTracking`, `Observation` e `Incident` (en `business.models.expeditions`); `Expedition.start`, `suspend`, `resume`, `finish`, `startActivity`, `finishActivity`, `recordObservation` y `reportIncident`; `InvalidTrackingException`.
- **Por qué:** el enunciado pide registrar comienzo, finalización, observaciones, incidentes y resultados, que es el estado real de un ítem del itinerario y no lo planificado: `ItineraryItem` sabe cuándo *debía* hacerse y `ActivityTracking` cuándo se hizo, y un ítem sin tracking es una actividad que todavía no arrancó, así que el `Optional` distingue "no empezó" de "empezó y no terminó". Sólo se registra con la expedición en ejecución, y los métodos que mutan el tracking son package-private: igual que las asignaciones (D19), la expedición es la única puerta, porque la regla depende de su estado. `Observation` e `Incident` son conceptos distintos aunque tengan la misma forma, porque una observación es información y un incidente es lo que dispara una replanificación, y separarlos permite que el informe cuente incidentes sin filtrar texto libre. Al terminar una actividad se consume lo asignado, y si no alcanza la actividad no se cierra (D30): es el único momento en que el stock baja de verdad, porque antes era una previsión (D4) y la validación sólo comparaba contra el stock disponible, y así el informe puede contrastar consumo estimado contra consumo real. `startedOutOfSchedule` usa `TimePeriod.includes` para detectar una actividad que arrancó fuera de su franja, pero no lo impide: en el campo los retrasos existen, y lo que corresponde ante un desvío es replanificar, no rechazar el registro de lo que pasó.
- **Alternativas descartadas:** campos sueltos en `ItineraryItem` (`startedAt`, `finishedAt`, listas de notas) — mezclaría el plan con la ejecución en una clase que ya tiene las asignaciones, y obligaría a chequear nulos en cada lugar. Exigir que el comienzo real caiga dentro del período agendado — haría imposible registrar lo que realmente pasó, que es justamente para lo que sirve el seguimiento. Consumir los consumibles al asignarlos o al aprobar — descontaría stock de algo que todavía no ocurrió, y una expedición suspendida o replanificada dejaría el catálogo mal.

### D24 — El informe es una proyección que no calcula nada por su cuenta

- **Patrón / principio:** DRY + separación entre estado y proyección; Dependency Injection por constructor.
- **Dónde:** `ExpeditionReporter` (en `business.reports`) y su resultado `ExpeditionReport` (en `business.models.report`); `ItineraryItem.supplyFor`.
- **Por qué:** el informe que pide el enunciado (resumen operativo, consumo de recursos, riesgos e incidentes) es la misma información que ya tienen la estimación (D4) y el seguimiento (D23), vista junta. Por eso el reporter recibe el `ExpeditionEstimator` en el constructor y le delega todo lo estimado en vez de recalcularlo: si cambia cómo se combina el riesgo, el informe no se entera. Lo único propio del informe es contrastar el plan con lo que pasó: el consumo real sale de las actividades terminadas y el estimado de todas, así que un itinerario a medio ejecutar muestra los dos números distintos. `ExpeditionReport` es un `record` inmutable y sin builder porque no protege ningún invariante: es una foto de datos que ya validó otro. La regla de qué consumible cubre cada requisito pasó a `ItineraryItem.supplyFor`, porque la necesitaban tres clientes distintos (la validación de consumibles, el consumo real al terminar una actividad y el informe) y repetirla habría sido la duplicación que más caro se paga: la que se desincroniza sin que ningún test la note.
- **Alternativas descartadas:** que el informe recorra el itinerario y calcule duración y riesgo por su cuenta — duplicaría las reglas de agregación de D4. Que sea `Expedition.report()` — mezclaría en la entidad una responsabilidad de presentación que cambia por motivos distintos (SRP), y obligaría a la expedición a conocer al estimador. Guardar el informe dentro de la expedición — quedaría desactualizado apenas se registre algo nuevo, cuando generarlo cuesta lo mismo que leerlo.

### D25 — Replanificación: reutilizar el builder para construir una alternativa en borrador

- **Patrón / principio:** reutilización de Builder (D3) y de las puertas ya existentes (D19, D21) en vez de una regla nueva.
- **Dónde:** `ExpeditionReplanner` (en `business.replanning`), único método `planAlternative(Expedition original, ExpeditionId alternativeId, ItineraryItem affectedItem)`.
- **Por qué:**
  - D20 ya había decidido que una expedición aprobada no vuelve a borrador: si hay que cambiarla, se construye una alternativa. Esta clase es esa construcción: arma un `ExpeditionBuilder` con los datos generales de la original (objetivos, zonas, responsables, restricciones, permisos), agenda de nuevo cada ítem salvo el afectado y copia sus asignaciones. El resultado es una `Expedition` nueva en borrador.
  - Cancelación, indisponibilidad y retraso —los tres motivos que pide el enunciado— no necesitan un método por caso: las tres se resuelven con el mismo primitivo, "sacar el ítem afectado del plan y dejarlo en borrador". Para una cancelación, el responsable no vuelve a agendar esa actividad; para un retraso, la reagenda en otro horario con `schedule` (D21); para una indisponibilidad, la reagenda y asigna otro recurso con `assignStaff`/`assignEquipment`/`assignSupply` (D19). Ningún caso pide una heurística de reprogramación automática, así que no hace falta una por motivo.
  - Reutiliza tal cual el chequeo de dependencias de `Itinerary.add` (D21): si una actividad ya agendada depende de la afectada, construir la alternativa falla con `InvalidScheduleException`, señalando que esa dependencia también hay que resolverla antes de poder generar la alternativa.
  - La original no se toca: son dos expediciones distintas por identidad (D15), así que la original queda intacta para historial o auditoría en vez de reemplazarse.
- **Alternativas descartadas:**
  - Mutar la expedición original in situ — contradice D20.
  - Reagendar automáticamente la actividad afectada eligiendo horario y recursos — el enunciado pide "construir una alternativa" para que un responsable la ajuste, no un algoritmo de reprogramación; además duplicaría la lógica de asignación (D18) sin un criterio de selección definido por el enunciado.
  - Guardar el motivo de la replanificación en `Expedition` — hoy ninguna regla ni informe lo consume, mismo criterio que los objetivos como texto libre (D16); se agrega si aparece un caso concreto que lo necesite.
- **Consecuencia:** si el ítem afectado tiene dependientes ya agendados, `planAlternative` falla en vez de dejarlos huérfanos o quitarlos en cascada; quien replanifica tiene que decidir explícitamente qué hacer con ellos primero.


### D26 — El id de la expedición como value object (corrección de la Entrega 1)

- **Origen:** corrección de la Entrega 1, punto 5 (el id de la expedición lo inventa el cliente). Evaluamos el hallazgo y convertimos el id en un value object, que es lo que D16 preveía cuando un id ganara reglas propias.
- **Patrón / principio:** value object (D8).
- **Dónde:** `ExpeditionId`, usado por `Expedition`, `ExpeditionBuilder.withId` y `ExpeditionReplanner.planAlternative`.
- **Por qué:** las reservas usan este id para saber qué expedición las hizo (D27). Con un `String` suelto se puede pasar el id de un recurso donde va el de una expedición; con `ExpeditionId` el compilador lo rechaza. Como `record`, compara por valor (D15). Un valor en blanco falla en el propio id y uno nulo en `Expedition`, con la división de excepciones de D16. `toString()` devuelve solo el valor, para que los mensajes digan `e-1`.
- **Alternativas descartadas:** dejar el `String` y controlar solo la unicidad — no evita confundirlo con otro id.


### D27 — Reservas con dueño, liberables y todo o nada; indisponibilidades aparte (corrección de la Entrega 1)

- **Origen:** corrección de la Entrega 1, punto 1 (las reservas no tienen dueño y no se pueden liberar; tampoco se distingue una reserva de una indisponibilidad, como una licencia o un mantenimiento, y terminar antes una expedición no libera sus horarios). Evaluamos la sugerencia del informe (que la reserva tenga dueño, `Reservation(expeditionId, period)`, que se pueda liberar lo de una expedición y que chequear y reservar sea una sola operación de un servicio) y la seguimos.
- **Patrón / principio:** value object (D8) + Information Expert (el calendario es el registro de compromisos, así que es quien libera) + servicio de dominio para la operación que toca varios recursos a la vez.
- **Dónde:** `Reservation(ExpeditionId, TimePeriod)` y `Unavailability(TimePeriod, reason)` en `business.models.resources`; `AvailabilityCalendar.book(Reservation)`, `releaseAll(ExpeditionId)` y `markUnavailable(Unavailability)`; `ReusableResource.reserve(Reservation)`, `releaseAll(ExpeditionId)` y `markUnavailable(Unavailability)`; `ResourceReservations.requireReservable(Expedition)`, `reserveFor(Expedition)` y `releaseFor(Expedition)` en `business.services`, invocados desde los casos de uso `ApproveExpedition` y `FinishExpedition` (D28).
- **Por qué:**
  - Reservar es comprometer un recurso *para alguien*. El calendario guardaba `TimePeriod` sueltos, así que no podía responder de quién era una franja ni deshacerla cuando esa expedición ya no la necesitaba. Con el dueño en la reserva, `releaseAll` libera exactamente lo de una expedición y deja intacto lo de las demás. Liberar una expedición que no tiene reservas no es un error (mismo criterio que repetir una asignación, D19).
  - La regla de disponibilidad no cambia: una franja reservada no está libre para nadie, sin importar quién la reservó.
  - Una indisponibilidad (licencia, mantenimiento) también ocupa el calendario, pero no es un compromiso con una expedición: no tiene dueño, liberar una expedición no la levanta, y lleva su motivo. Registrarla nunca falla, aunque pise una reserva existente, porque es algo que pasó y no un pedido; la validación de superposición la reporta como recurso no disponible (`"... is not available during ..."`).
  - Aprobar reserva varios recursos de una vez, y antes una falla a mitad de camino dejaba reservada una parte. `ResourceReservations` arma todos los compromisos de la expedición, los verifica contra los calendarios **y entre sí** (dos ítems propios que comparten un recurso en horarios superpuestos), y recién si todo da, reserva. Si algo falla, no reserva nada.
  - `requireReservable` hace la misma verificación sin reservar, para que el caso de uso pueda comprobar que todo se puede reservar antes de aprobar.
  - Al terminar, `releaseFor` libera todas las reservas de la expedición en cada recurso que usa. Así una expedición que termina antes devuelve los horarios de las actividades que no se hicieron. Libera también las franjas ya pasadas, que no cambian nada porque nadie reserva hacia atrás.
- **Alternativas descartadas:**
  - Guardar en `Expedition` qué recursos reservó — repartiría el registro de compromisos entre dos objetos que se pueden desincronizar, y el informe señala justamente que el calendario tiene que ser ese registro.
  - Liberar por período (`release(TimePeriod)`) — no distingue de quién es la franja, que es justamente lo que faltaba.
  - Modelar la indisponibilidad como una `Reservation` con un dueño ficticio — mezclaría dos conceptos y un `releaseAll` de ese dueño la levantaría.
  - Rechazar una indisponibilidad que pisa una reserva — impediría registrar lo que realmente pasó, mismo criterio que D23 con los retrasos.
  - Reservar de a uno y, si algo falla, liberar lo reservado — deja un momento en que el recurso figura ocupado y depende de que la vuelta atrás no falle.
  - Que `Expedition.approve` reciba el servicio y reserve adentro — volvería a poner en la entidad una operación que modifica varios recursos, que es la causa que señaló el informe.

### D28 — Casos de uso para las operaciones que coordinan varios objetos (corrección de la Entrega 1)

- **Origen:** corrección de la Entrega 1, punto 7 (dónde viven los casos de uso) y punto 1 (el servicio de reservas, "invocado desde el caso de uso de aprobación"). Evaluamos la sugerencia del informe y la seguimos con `ApproveExpedition` y `FinishExpedition`.
- **Patrón / principio:** caso de uso (capa de aplicación) + inyección de dependencias por constructor.
- **Dónde:** en `business.usecases`: `ApproveExpedition`, que recibe la `ApprovalPolicy` y el `ResourceReservations` en el constructor, y `FinishExpedition`, que recibe el `ResourceReservations`.
- **Por qué:**
  - Una operación es método de la entidad cuando toca un solo objeto y no necesita nada de afuera (`schedule`, `assign*`, `startActivity`). Aprobar no es así: cambia el estado de la expedición y el calendario de cada recurso que usa, y necesita un validador. Esa coordinación es un caso de uso.
  - El orden importa: primero `requireReservable` (verifica sin reservar), después `Expedition.approve` (valida, exige las advertencias aceptadas y pasa a aprobada) y recién al final `reserveFor`. Si falla la verificación o la validación, no cambió nada. Si se aprobara antes de verificar, una reserva fallida dejaría una expedición aprobada sin recursos.
  - La política y el servicio llegan por constructor: quien aprueba no elige con qué validar en cada llamada, y la política ya trae las reglas obligatorias (D31).
  - Terminar también cambia la expedición y los calendarios de sus recursos. `FinishExpedition` primero termina la expedición y después libera: si la transición no es válida, falla antes de liberar nada, y liberar no puede fallar.
- **Alternativas descartadas:** que el caso de uso reserve primero y libere si la validación falla — es la vuelta atrás que D27 ya descartó.
- **Consecuencia:** `Expedition.approve` y `Expedition.finish` siguen siendo públicos y no tocan los calendarios; la aprobación y la finalización completas son las de los casos de uso.

### D29 — No se termina una expedición con actividades en curso (corrección de la Entrega 1)

- **Origen:** corrección de la Entrega 1, pregunta abierta "¿qué pasa con una actividad en curso cuando la expedición finaliza?". Hasta ahora `finish()` solo consultaba la tabla de estados: con una actividad iniciada y sin cerrar, la expedición quedaba terminada, el consumible no se descontaba y el informe quedaba incompleto. El informe plantea tres opciones (cerrar en cascada, exigir cerrar antes o registrarla como abandonada); elegimos exigir cerrar antes.
- **Patrón / principio:** fail fast + Information Expert (el ítem sabe si está en curso).
- **Dónde:** `Expedition.finish()`, `ItineraryItem.isInProgress()` y `ActivitiesInProgressException`. Vale tanto desde `IN_EXECUTION` como desde `SUSPENDED`.
- **Por qué:** cerrar una actividad es registrar cuándo terminó y con qué resultado, y consumir lo que usó (D23). Ninguna de las dos cosas se puede inventar al terminar la expedición: solo las sabe quien la ejecutó. Exigir que se cierre antes mantiene el seguimiento y el stock fieles a lo que pasó. El chequeo va antes de la transición de siempre (`transitionTo`): solo puede haber actividades en curso en `IN_EXECUTION` o `SUSPENDED`, desde donde terminar es válido, así que el orden no cambia qué error se reporta. La excepción nombra las actividades en curso para que se sepa qué falta cerrar.
- **Alternativas descartadas:**
  - Cerrar en cascada — habría que inventar la hora de fin y el resultado, y consumir como si la actividad se hubiera completado.
  - Registrarla como abandonada — suma un estado nuevo al seguimiento que ninguna regla ni el informe usan hoy.

### D30 — Stock reservado por expedición (corrección de la Entrega 1)

- **Origen:** corrección de la Entrega 1, punto 2 (los consumibles no se reservan). Dos expediciones aprobadas podían contar con el mismo stock porque los consumibles solo tenían stock actual. Evaluamos la sugerencia del informe (reservar el consumible con dueño, consumir contra esa reserva y liberar el sobrante) y la seguimos.
- **Patrón / principio:** Information Expert (el consumible lleva la cuenta de su stock y de lo reservado) + mismo criterio de dueño que las reservas de recursos reutilizables (D27).
- **Dónde:** `Depletable` guarda lo reservado por cada `ExpeditionId`; `DepletableResource.reserve(ExpeditionId, Quantity)`, `canConsume(ExpeditionId, Quantity)`, `consume(ExpeditionId, Quantity)` y `releaseAll(ExpeditionId)`; `Expedition.finishActivity`; `hasStockFor` responde por el stock disponible. `ResourceReservations` reserva el stock al aprobar (`ApproveExpedition`) y lo libera al terminar (`FinishExpedition`); `Itinerary.supplyDemands()` y `SupplyDemand` calculan cuánto pide el itinerario de cada consumible.
- **Por qué:**
  - Reservar no es consumir: el stock real (`getStock()`) baja recién cuando se usa (D23), pero lo reservado ya no está disponible para nadie más. `hasStockFor` mira el disponible (stock menos todo lo reservado), así que el catálogo y la validación de consumibles ven lo que otras expediciones ya tienen tomado sin cambiar su código.
  - Consumir toma primero de lo que esa expedición reservó y, si hace falta más, del stock libre. Nunca toca lo reservado por otra expedición. Si no alcanza, falla sin modificar nada.
  - Se reserva al aprobar, en el mismo todo o nada que los recursos reutilizables (D27): si un consumible no tiene stock disponible para lo que pide el itinerario, no se reserva nada. Así dos expediciones aprobadas ya no pueden contar con el mismo stock, y la validación de la segunda ve lo que reservó la primera.
  - `releaseAll` devuelve lo reservado que no se usó, que es el "sobrante" del informe. Lo libera `FinishExpedition` al terminar la expedición y no cada actividad al cerrarse: la reserva es el total del itinerario, y liberar al cerrar una actividad dejaría sin stock a otra que usa el mismo consumible. Igual que con las reservas de recursos reutilizables, liberar lo que no existe no es un error.
  - El cálculo de cuánto pide el itinerario de cada consumible se movió de la validación a `Itinerary.supplyDemands()`, porque ahora también lo necesita la reserva; tenerlo en dos lugares sería duplicar la regla. Si el itinerario pide un consumible en otra unidad, la reserva lo deja pasar y lo reporta la validación como crítico, igual que antes (no se suman unidades distintas). Cada ítem calcula su propia demanda (`ItineraryItem.supplyDemands()`) y el itinerario junta la de todos con `SupplyDemand.combine`, así el agrupamiento vive en un solo lugar.
  - Cerrar una actividad es todo o nada: `finishActivity` primero verifica que el cierre sea válido (`ActivityTracking.requireCanFinish`), después verifica que alcance el stock de **todos** los consumibles de la actividad (`canConsume`), recién entonces consume y, al final, marca la actividad como terminada. Antes la marcaba terminada y después consumía, así que un consumo fallido dejaba una actividad cerrada sin haber gastado nada; y con dos consumibles, el primero se descontaba aunque el segundo no alcanzara.
  - El nombre es el mismo que en los recursos reutilizables (`reserve`), porque es la misma idea: apartar algo para una expedición. Lo que cambia es la medida, una franja horaria o una cantidad.
- **Alternativas descartadas:**
  - Descontar el stock al aprobar — mezclaría lo previsto con lo usado, y el informe no podría contrastar consumo estimado contra real (D23, D24).
  - Exigir que todo consumo esté reservado antes — en el campo se puede usar más de lo previsto, y eso tiene que poder registrarse mientras haya stock libre.

### D31 — Las reglas obligatorias de aprobación las define el dominio (corrección de la Entrega 1)

- **Origen:** corrección de la Entrega 1, punto 3 (aprobar sin errores críticos dependía del validador que eligiera quien llama). `approve` aceptaba cualquier validador, y una inmersión sin buzos se aprobaba con uno vacío. Evaluamos la sugerencia del informe (que el dominio defina las reglas obligatorias y que quien llama solo pueda sumar reglas encima) y la seguimos.
- **Patrón / principio:** Policy, con un mínimo fijo del dominio + Open/Closed para sumar reglas + Dependency Inversion (la entidad sigue dependiendo de la interfaz).
- **Dónde:** `ApprovalPolicy` (en `business.validations`, antes `ValidationOrchestrator`), clase `final` que implementa `ExpeditionValidator`, con las siete reglas como obligatorias y un constructor que solo recibe reglas adicionales. `ApproveExpedition` recibe la `ApprovalPolicy` concreta (D28).
- **Por qué:**
  - "Una expedición con errores críticos no puede aprobarse" es la regla central del enunciado, y qué cuenta como crítico tiene que decidirlo el dominio, no quien llama. Las siete reglas viven dentro de la política y corren siempre; quien la arma puede agregar reglas (por ejemplo, más estrictas para un tipo de expedición), nunca sacar.
  - Como sugiere el informe (punto 7), el dueño del armado del validador es el caso de uso: `ApproveExpedition` pide una `ApprovalPolicy` y no cualquier validador, y la política es `final`, así que quien aprueba por el caso de uso no puede saltear el mínimo.
  - `Expedition.approve` sigue recibiendo la interfaz `ExpeditionValidator` (D22): la entidad no conoce la política concreta ni sus reglas, y no hay un ciclo de dependencias entre `models.expeditions` y `validations`.
- **Alternativas descartadas:**
  - Que `Expedition.approve` pida la `ApprovalPolicy` concreta — garantizaría el mínimo también al llamar directo a la entidad, pero ataría la entidad a la política y a sus siete reglas, y crearía un ciclo entre paquetes (las reglas leen la expedición y la expedición conocería la política).
  - Que la política exponga sus reglas o permita quitar alguna — volvería a dejar la decisión en quien llama.
- **Consecuencia:** `Expedition.approve` sigue aceptando cualquier `ExpeditionValidator`, así que llamarla directo con un validador vacío aprueba sin las reglas obligatorias. Es el mismo criterio que con las reservas (D28): la aprobación completa es la de `ApproveExpedition`.

### D32 — Un resultado de validación se identifica por su regla y su sujeto (corrección de la Entrega 1)

- **Origen:** corrección de la Entrega 1, punto 4 (un resultado de validación se identifica por su texto). Una aceptación se podía armar tipeando el mensaje sin haber visto la advertencia, y si cambiaba la redacción todas las aceptaciones dejaban de coincidir. Evaluamos la sugerencia del informe (que el resultado identifique la regla que lo produjo y su sujeto, la actividad y si corresponde el recurso, con ids tipados, y que la aceptación compare esa identidad) y la seguimos.
- **Patrón / principio:** value objects (D8) + identidad explícita en vez de igualdad de texto.
- **Dónde:** `ActivityId` y `ResourceId` (ids tipados, usados por `Activity` y por `Resource.getId()`, `Person`, `Vehicle`, `Instrument`, `Depletable`, `ResourceCatalog` y las excepciones de recursos); `RuleId` y `ValidationSubject` (en `business.models.validation`); `ValidationResult(severity, rule, subject, message)` con `isSameFindingAs`; `AcceptedWarning.covers`; cada regla declara su `ID`.
- **Por qué:**
  - Un hallazgo es "esta regla encontró esto sobre esta actividad / este recurso". El mensaje es solo cómo se muestra. `isSameFindingAs` compara regla y sujeto, y `AcceptedWarning.covers` usa eso: cambiar la redacción de un mensaje ya no invalida las aceptaciones, y una aceptación para otra actividad no cubre la advertencia.
  - El sujeto tiene la actividad y el recurso opcionales, porque no todo hallazgo es de una actividad: la capacidad y los permisos son de la expedición entera (`ValidationSubject.expedition()`), y el stock de un consumible es del recurso. Se arma con factorías (`of(actividad)`, `of(recurso)`, `of(actividad, recurso)`) para no tener que pasar vacíos a mano.
  - Cada regla declara su `RuleId` como constante propia (`ResourceOverlapValidation.ID`). No es un enum: sumar una regla sigue siendo sumar una clase (Open/Closed).
  - Los ids tipados evitan cruzar el id de una actividad con el de un recurso en un mismo sujeto, que con dos `String` pasaría sin que nada lo note. Igual que `ExpeditionId`: comparan por valor (D15), un valor en blanco falla en el propio id y uno nulo en la entidad (D16), y `toString()` devuelve solo el valor. El id de un recurso es natural (una patente, un documento) y su unicidad la sigue garantizando el catálogo (D18).
- **Alternativas descartadas:**
  - Un enum de reglas — cada regla nueva obligaría a modificarlo.
  - Un único `EntityId` para actividades y recursos — no evitaría cruzarlos.
  - Que la aceptación guarde solo la clave (regla y sujeto) en vez del resultado — el informe de la expedición necesita mostrar qué advertencia se aceptó.
- **Consecuencia:** dos resultados críticos de una misma regla sobre la misma actividad (por ejemplo, faltan personas y falta equipo) pueden tener el mismo sujeto. No es un problema, porque lo crítico no se acepta; las advertencias sí son una por actividad. El id de `Zone` queda como `String`: ninguna regla lo usa.

## Patrones que decidimos no aplicar

| Patrón | Por qué no                                                                                                                                                                                              | Consecuencias |
|--------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| Decorator | Para "advertencia aceptada con justificación" alcanza con `AcceptedWarning`, un `record` que referencia el resultado y le suma responsable y justificación (D22). No hace falta que la advertencia aceptada se comporte como un `ValidationResult`. | Si más adelante se necesita apilar comportamiento sobre un resultado de validación (ej. distintos tipos de aceptación con reglas propias), habría que reconsiderarlo. |
| State (máquina de estados formal) | Las transiciones válidas son una tabla dentro de `ExpeditionStatus` (D20), y hoy lo único que cambia según el estado es si el itinerario se puede modificar (D19). Una clase por estado agregaría seis clases para responder dos preguntas. | Si cada estado empieza a habilitar o prohibir muchas operaciones distintas (ej. qué se puede registrar en ejecución o durante una suspensión), los chequeos de estado se repetirían en `Expedition` y convendría pasar a State. |
| Strategy de conversión entre sistemas de medida | Consideramos una posible extensión a futuro con más de un sistema de medida (ej. métrico e imperial: litros y galones, kilogramos y libras). Hoy hay un único sistema, así que `MeasurementUnit` es un enum simple y `Quantity` rechaza operar entre unidades distintas en vez de convertirlas. | Si se agrega otro sistema, habría que introducir una abstracción de conversión (ej. `UnitConverter`) que `Quantity` use para normalizar antes de sumar, restar o comparar. Como hoy toda la aritmética de unidades está encapsulada en `Quantity` (D8), el cambio queda en ese value object y no se propaga al resto del dominio. |
| Servicio dedicado de asignación (`AsignacionService`) | El enunciado pide "proponer o realizar asignaciones", y ya está cubierto sin una clase intermedia: `ResourceCatalog` (D18) responde quién puede cubrir un requisito (Information Expert sobre el catálogo) y `Expedition.assignStaff`/`assignEquipment`/`assignSupply` (D19) realiza la asignación verificando el estado. Un servicio aparte solo delegaría en las dos sin agregar comportamiento propio. | Si aparece una regla propia de asignación (ej. elegir entre varios candidatos con un criterio de optimización, no solo listarlos), ahí se justifica introducir un servicio que la encapsule. |

## Supuestos

- La disponibilidad de personas/vehículos/instrumentos se valida por franjas horarias dentro del período de la expedición (permitiendo participar de más de una expedición si los horarios no chocan), no por exclusividad total del recurso durante todo el período: las personas son parte del catálogo de recursos, la dificultad para asignarlas es la temporalidad, y el enunciado no restringe una persona a una expedición a la vez.
- "Permisos" y "certificaciones" son conceptos distintos: permisos a nivel expedición/zona (autorización regulatoria), certificaciones a nivel recurso (habilitación puntual). El enunciado les da entidades distintas, así que se modelan como conceptos separados.
- Las certificaciones aplican también a vehículos e instrumentos, no solo a personas: la capacitación de una persona y la habilitación técnica de un vehículo o un instrumento son la misma regla de negocio, una habilitación que el recurso tiene o no y que una actividad puede exigir. Modelarlas dos veces duplicaría el concepto, así que las tres entidades implementan `Certifiable` (D10).
- Los consumibles se modelan como un tipo de recurso aparte de personal/vehículos/instrumentos, con disponibilidad por stock (`DepletableResource`) en vez de por tiempo (`ReusableResource`). Son recursos del catálogo como cualquier otro, pero con su propio criterio de disponibilidad.
- "Disponibilidad" en el catálogo de recursos no es un único concepto: para recursos reutilizables es disponibilidad temporal, para consumibles es stock. Un consumible, una vez consumido, deja de ser aprovechable y no puede usarse en otra expedición, a diferencia de un vehículo o una persona. Es lo que sostiene los dos contratos separados de D7.
- Los valores de `SampleCollectionRules` y `DivingRules` (tiempos, umbral de profundidad, cantidad de buzos y equipamiento) son ilustrativos: el enunciado no los define.
- Las restricciones de una actividad todavía no se modelan: ninguna regla las usa y no está definido qué forma tienen. Se agregan (como value object, no como `String`) cuando una validación las necesite.
- `Activity.dependsOn` responde solo por las dependencias **directas**, no por las transitivas: hoy nadie recorre el grafo. No hace falta detectar ciclos porque son imposibles por construcción — `dependencies` es `final`, se copia con `List.copyOf` en el constructor y no hay setter, así que una actividad solo puede depender de otras que ya existían cuando se la creó. El orden se resuelve en el itinerario: al agendar se exige que cada dependencia directa ya esté agendada y termine antes (D21), y como eso se cumple para cada ítem, también se cumple para las transitivas.
- Los objetivos de la expedición son texto libre: ninguna regla los usa, así que no se reifican (mismo criterio que D16 con los ids).
- Las restricciones generales de la expedición se interpretan como un máximo de participantes y una tolerancia de riesgo (`ExpeditionRestrictions`): el enunciado las pide pero no define cuáles son.
- La capacidad de la expedición se mide como la cantidad de personas distintas asignadas a su itinerario: una persona asignada a varias actividades cuenta una sola vez, y los responsables cuentan solo si están asignados a alguna actividad.
- Para calcular el stock necesario, cada requisito de consumible se descuenta del primer consumible asignado al ítem que sea de su categoría.
- Un consumible se mide en una sola unidad, la de su stock (`DepletableResource.unit()`). Si el itinerario lo pide en otra, `InsufficientSuppliesValidation` lo reporta como crítica en vez de fallar: es un catálogo mal cargado, no un faltante, pero tiene que salir junto con los demás problemas y no cortar la validación (D14).
- Las validaciones de personal evalúan cada requisito por separado: una misma persona asignada puede contar para dos requisitos distintos de la misma actividad. Con dos requisitos de un buzo cada uno y un solo buzo asignado, los dos se dan por cubiertos. Resolverlo con exactitud es un problema de asignación uno a uno (matching) que el enunciado no justifica; si hiciera falta, la regla viviría en `StaffRequirement` y no en la validación.
- La validación de superposición también compara contra las reservas de otras expediciones ya aprobadas, así que tiene sentido correrla antes de aprobar: una vez aprobada, la expedición tiene sus propias reservas.