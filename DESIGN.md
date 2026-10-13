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
- **Revisado en D44:** ya son seis tipos (se sumaron `DroneSurveyRules`, `WildlifeObservationRules`, `WaterQualityMeasurementRules` y `TransferRules`) y cada uno se puede crear por su nombre con el registro de tipos. `ActivityRules` sumó dos preguntas con respuesta vacía por defecto (D43) y un Decorator la envuelve para la variante nocturna (D42). La decisión no cambia: agregar un tipo sigue siendo agregar una clase.


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
  - El builder no elige la identidad de la expedición: `build(ExpeditionRepository)` le pide el id al repositorio con `nextId()`, así que quien arma la expedición no lo inventa (D26).
  - El builder no repite validaciones. `build(ExpeditionRepository)` llama al constructor, que aplica los guards de D16 (textos obligatorios, colecciones obligatorias y no vacías), y agenda las actividades con el mismo `Expedition.schedule` que se usa después de crear la expedición. Así las reglas para agendar (D21) viven en un solo lugar, se agende desde el builder o después.
  - La validación de negocio pesada corre aparte, en `Expedition.approve` con la `ApprovalPolicy` (D22, D31), antes de permitir el paso a "Approved" — así el builder no termina siendo responsable de todo.
- **Alternativas descartadas:**
  - Builder + Director — más estructura de la que hace falta para este caso.
  - Validar todas las reglas de negocio dentro de `build()` — mezclaría "está bien formado" con "es válido para operar", que tienen ciclos de vida distintos.
  - Que el builder reciba `ItineraryItem` ya armados para planificar — obligaría a que existan ítems fuera de una expedición y a repetir en el builder los chequeos de zona, período y dependencias. Por eso las actividades nuevas entran como actividad y horario (`addActivity`) y se agendan con `Expedition.schedule`. La única excepción es `addFinishedItem` (D33): recibe ítems ya terminados de otra expedición, porque lo que ya ocurrió no se puede volver a agendar; `build` los copia (`addFinishedCopyOf`) y no los vuelve a validar como plan.
- **Revisado en D39:** el builder ya no guarda actividades y terminados en dos listas: todo lo que se le agrega es un `PlannedNode` (una actividad con su horario, un bloque o un ítem terminado) y `build` los agenda con `Expedition.schedule(PlannedNode)` en el orden en que se agregaron, en vez de los terminados primero. `Expedition.addFinishedCopyOf` e `Itinerary.addFinishedCopyOf` ya no existen: `addFinishedItem` queda como atajo de `addNode(PlannedNode.finished(item))`. La decisión no cambia: lo terminado se copia y no se vuelve a validar como plan.


### D4 — Estimación como agregación, Informe como caso de uso que reutiliza

- **Patrón / principio:** evitar duplicación (DRY) reutilizando Strategy de D1.
- **Dónde:** `ExpeditionEstimator` (en `business.estimations`) y su resultado `ExpeditionEstimate` (en `business.models.estimation`); el informe (`ExpeditionReporter` + `ExpeditionReport`) reutiliza este servicio.
- **Por qué:** la estimación no calcula nada propio: recorre el itinerario y le pregunta a cada actividad, que a su vez delega en su `ActivityRules` (D1), así que agregar un tipo de actividad no toca al estimador. Las tres agregaciones tienen reglas distintas: la duración se suma, el riesgo global es el más alto de sus actividades (`RiskLevel.isAbove`) y el consumo se acumula por categoría sumando `Quantity`, que es la aritmética que ya existe (D8). Estimar no cambia nada ni exige un estado, así que se puede estimar un borrador, que es cuando más sirve para decidir. El equipamiento no se agrega: sumar "1 bote" de tres actividades daría 3 botes cuando puede ser el mismo reutilizado, y lo que hace falta por actividad ya lo verifica `MissingResourcesValidation` (D2).
- **Alternativas descartadas:** calcular el resumen de riesgos del informe con lógica propia, independiente de `ExpeditionEstimator` — duplicaría reglas de cálculo. Builder para `ExpeditionEstimate` — no tiene invariantes que proteger, es una proyección de datos. Que la duración total sea el lapso entre el primer inicio y el último fin del itinerario — eso mide cuánto dura el plan en el calendario y no cuánto trabajo hay, porque los huecos entre actividades no son esfuerzo estimado.
- **Revisado en D39:** la duración ya no es una suma plana de todas las actividades. El estimador agrega sobre los nodos raíz del itinerario, y cada bloque combina a sus hijos: suma en secuencial y máximo en paralelo. Un itinerario sin bloques da el mismo resultado que antes. El riesgo y el consumo se siguen combinando igual (el más alto y la suma).


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
- **Revisado en D39:** el itinerario pasó de colección ordenada de ítems a árbol: guarda nodos raíz (`ItineraryNode`), que son actividades agendadas o bloques. `getItems()` sigue devolviendo las hojas ordenadas por hora de inicio, pero ya no es una vista de la lista interna sino una copia no modificable calculada en cada llamada: las raíces se guardan en el orden en que se agendaron y se ordenan al leer. `getNodes()` devuelve las raíces.


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
- **Revisado en D43:** una actividad puede exigir certificaciones a cada persona asignada, y no solo a las que cubren un requisito de personal. La pregunta sigue pasando por `Certifiable.hasCertification`, sin conocer la clase concreta.


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
- **Revisado en D43:** se dio el caso que anticipa la consecuencia de arriba. Los permisos que exige una actividad dependen de su zona, así que `ActivityRules.requiredPermitsIn(Zone)` recibe la zona como parámetro. Es el único método con parámetro; los demás no cambiaron.


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
- **Revisado en D43:** la zona sigue siendo dueña de sus permisos y ahora también nombra el permiso especial que exige para un tipo de operación (`Zone.specialPermitFor`). Además de las zonas, una actividad puede exigir permisos en su zona, y `MissingPermitsValidation` reporta los dos.


### D14 — Las preguntas devuelven `boolean`, los comandos fallan con excepción

- **Patrón / principio:** Command-Query Separation + fail fast.
- **Dónde:** `ReusableResource.isAvailableDuring` / `reserve`, `DepletableResource.hasStockFor` / `consume`; `ResourceUnavailableException` e `InsufficientStockException` en `business.exceptions`.
- **Por qué:** cada contrato de recurso (D7) tiene una consulta y un comando. La consulta responde `boolean` porque es exactamente lo que las validaciones (D2) necesitan para decidir sin romper nada: corren sobre una expedición en borrador y tienen que poder juntar **todos** los problemas, no frenarse en el primero. El comando, en cambio, no tiene forma válida de "fallar a medias": si `reserve` o `consume` se ejecutan sobre un recurso que no da, es un error de programación (alguien no consultó antes), y devolver `false` lo dejaría pasar silenciosamente. Por eso son excepciones *unchecked*: no son casos de negocio esperados que el llamador deba manejar, son invariantes rotos. `Depletable.consume` verifica antes de modificar, así que el stock queda intacto cuando falla.
- **Límite conocido:** `hasStockFor` sí lanza `IllegalArgumentException` si la unidad no coincide, porque `Quantity` no opera entre unidades distintas (D8). No es una excepción a la regla de arriba: "no alcanza el stock" es una respuesta válida (`false`), pero "¿me alcanzan 20 litros para 1 kilogramo?" es una pregunta mal formada, no un problema de la expedición. La validación de recursos faltantes (D2) tiene que comparar contra requisitos de la misma unidad; si llega a necesitar tolerar el desajuste, va a hacer falta un `UnitConverter` (ver la tabla de patrones no aplicados).
- **Alternativas descartadas:** comandos que devuelven `boolean` — el `if` de chequeo es fácil de omitir y el error aparece mucho después. Excepciones *checked* — obligarían a un `try/catch` en cada llamador que ya consultó y sabe que va a andar. Devolver `Optional`/`Result` — suma ceremonia sin agregar información: acá no hay más de un motivo de falla por operación.


### D15 — Entidades por identidad (clases), value objects por valor (`record`)

- **Patrón / principio:** distinción entidad / value object.
- **Dónde:** clases con identidad: `Person`, `Vehicle`, `Instrument`, `Depletable`, `Zone`, `Activity`, `Expedition`, `ItineraryItem`. `record` con igualdad por valor: `ExpeditionId`, `ActivityId`, `ResourceId`, `Reservation`, `Unavailability`, `TimePeriod`, `Quantity`, `Certification`, `Certifications`, `Permit`, `ResourceCategory`, `ReusableRequirement`, `DepletableRequirement`, `StaffRequirement`, `ExpeditionRestrictions`, `ValidationResult`, `RuleId`, `ValidationSubject`, `ApprovalResult`, `AcceptedWarning`, `Cancellation`, `Delay`, `ResourceWithdrawn`, `ReplannedActivity`.
- **Por qué:**
  - Las entidades tienen `id` pero **no** redefinen `equals`/`hashCode`: se comparan por identidad de instancia. Dos `Vehicle` con la misma patente son dos vehículos distintos del catálogo, y cada uno tiene su propio `AvailabilityCalendar` (D9) — igualarlos por `id` haría que reservar uno pareciera reservar al otro.
  - De ahí se apoya `Activity.dependsOn`: una dependencia apunta a *esa* actividad concreta, no a "cualquier actividad que se llame igual".
  - Los value objects son `record`: inmutables, sin identidad propia, intercambiables si valen lo mismo (D8).
- **Convención de accessors:** los `record` exponen sus componentes con el nombre del campo (`name()`, `amount()`), que es lo que genera el lenguaje. Las entidades exponen `getX()` generado por Lombok `@Getter`, para no escribir boilerplate a mano. Son dos estilos porque son dos categorías de objeto distintas, no un descuido.
- **`@Getter` va por campo, nunca sobre la clase:** anotar la clase expone *todo* el estado interno, y varias entidades tienen colaboradores que no deben salir. Queda privado lo que ya tiene un método que responde la pregunta: `requiredPermits` en `Zone` (se pregunta con `missingPermits`, D13), `calendar` en `ScheduledResource` (se pregunta con `isAvailableDuring`, D9), `certifications` en `Person`/`Vehicle`/`Instrument` (se pregunta con `hasCertification`, D10) y `rules` en `Activity` (se delega con `estimatedDuration` y compañía, D1). Exponerlos dejaría que un cliente se saltee la abstracción: si `Activity` devolviera su `ActivityRules`, el Strategy de D1 pasaría a ser público y cualquiera podría puentear la delegación.
- **Caso aparte — `Depletable.getStock()`:** este sí se expone. `stock` es un campo mutable (se reasigna al consumir) pero el `Quantity` que devuelve es inmutable (D8), así que entrega una foto del estado actual y no una referencia con la que romper invariantes. El informe de expedición (D4) necesita leer el consumo real, y no hay forma de obtenerlo preguntando `hasStockFor`.
- **Nota de build:** desde JDK 23 `javac` no corre los annotation processors que encuentra en el classpath. Lombok tiene que declararse explícitamente en el `<annotationProcessorPaths>` del `maven-compiler-plugin`; si falta, `@Getter` no genera nada y el proyecto no compila. El plugin ya activa el procesamiento por su cuenta al ver esa lista, así que no hace falta agregar `<proc>full</proc>`.
- **Alternativas descartadas:** `equals`/`hashCode` por `id` en las entidades — dos instancias con el mismo `id` pero distinto estado (distintas reservas, distinto stock) se tratarían como la misma. Hacer `record` todas las clases — los recursos son mutables por definición (se reservan, se consumen) y `Activity` referencia otras `Activity`, donde la identidad es justamente lo que importa. El caso menos obvio es `Zone`, que es inmutable y técnicamente podría ser un `record`: se deja como entidad porque pertenece a un catálogo y las expediciones la referencian por identidad — dos zonas con el mismo nombre son dos zonas distintas, no la misma.
- **Revisado en D46 y D48:** suma la entidad `ReplanProposal` y los `record` `Incident`, `IncidentId`, `IncidentReport`, `IncidentRegistered`, `ReplanProposalId`, `ReplanOption`, `ReplanDecision` y `ResourceReplaced`. `ResourceAssignment` es un value object que no es `record` porque guarda la forma de asignar su recurso; compara por el recurso.


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
- **Consecuencia:** es una clase concreta en memoria. Si más adelante hay persistencia, el contrato se extrae a `business/providers` con estas mismas consultas y esta clase pasa a ser un detalle, como ya se hizo con las expediciones (D26).


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
  - El enunciado define seis estados; el séptimo, reemplazada, lo agrega la replanificación (D33). Si el enum solo los listara, nada impediría pasar de borrador a en ejecución sin revisión ni aprobación. Las transiciones válidas son una tabla dentro del propio enum, así hay un único lugar donde leer el flujo completo:
    - borrador → revisión
    - revisión → borrador (se devuelve para corregir) o aprobada
    - aprobada → en ejecución o reemplazada (D33)
    - en ejecución → suspendida o finalizada
    - suspendida → en ejecución, finalizada o reemplazada (D33)
    - finalizada → ninguna
    - reemplazada → ninguna
  - `Expedition` le pregunta al enum antes de cada cambio y falla con excepción si la transición no es válida: cambiar de estado es un comando (D14). Terminar exige además que no quede ninguna actividad en curso (D29).
  - Una expedición aprobada no vuelve a borrador: si hay que cambiarla, se construye una alternativa (replanificación).
- **Alternativas descartadas:** un `switch` sobre el estado en cada método de `Expedition` — repetiría el flujo en varios lugares y cada estado nuevo obligaría a tocarlos todos. Una clase por estado — ver la tabla de patrones no aplicados.
- **Revisado en D47:** la tabla no cambió. Aceptar una propuesta de replanificación suspende la expedición en ejecución con las transiciones existentes (`suspendToReplan`) antes de armar la alternativa.


### D21 — Qué se rechaza al agendar y qué detecta la validación

- **Patrón / principio:** fail fast para planes mal formados + separación entre "está bien formado" y "es válido para operar" (mismo criterio que D3).
- **Dónde:** `Expedition.schedule`, `Itinerary.add` e `ItineraryItem` lanzan `InvalidScheduleException`; `TimePeriod.contains`.
- **Por qué:**
  - Al agendar se rechaza lo que no tiene sentido como plan, sin importar qué recursos haya: una actividad en una zona que no es de la expedición, un horario fuera del período de la expedición o fuera de la ventana de la actividad, la misma actividad dos veces, o una actividad cuya dependencia no está agendada o todavía no terminó. Tampoco se reasigna un ítem que ya ocurrió (D33). Son errores de quien arma el itinerario y conviene enterarse en el momento.
  - En cambio, la superposición de recursos, la falta de recursos, las certificaciones, los permisos y la capacidad dependen de asignaciones y del catálogo, que cambian mientras se planifica. Esos problemas se juntan todos con la validación (D2) en vez de frenar en el primero, porque un borrador puede estar incompleto a propósito.
  - Exigir que las dependencias ya estén agendadas obliga a armar el itinerario en orden, y a cambio garantiza que ninguna actividad queda antes de algo que necesita.
  - La contención de períodos vive en `TimePeriod.contains`, junto a `overlapsWith` y con el mismo intervalo semiabierto de D8: una actividad puede empezar justo cuando termina su dependencia.
- **Alternativas descartadas:**
  - Convertir todo en reglas de validación — permitiría armar itinerarios imposibles (una actividad antes de su dependencia) que recién se detectarían al pedir la aprobación.
  - Rechazar al agendar también la falta de recursos — impediría armar el itinerario antes de asignar.
- **Revisado en D40:** las mismas reglas valen para cada actividad de un bloque, y se suman las propias del bloque (vacío, orden de un secuencial, dependencia entre ramas paralelas). Un bloque entra entero, así que una dependencia puede estar agendada en el mismo bloque y no solo antes.


### D22 — La aprobación corre la validación y registra las advertencias aceptadas

- **Patrón / principio:** Information Expert (la expedición sabe cuándo puede aprobarse) + fail fast.
- **Dónde:** `Expedition.approve(List<ValidationRule> additionalRules, List<AcceptedWarning> acceptances)`, que corre `new ApprovalPolicy(additionalRules).validate(this)` (D31); `AcceptedWarning`; `ExpeditionNotApprovableException` y `UnacceptedWarningException`.
- **Por qué:**
  - El enunciado exige que una expedición con errores críticos no pueda pasar a aprobada. Para que eso no dependa de que alguien se acuerde de validar antes, ni de con qué validador lo haga, `approve` corre la validación en ese momento y con las reglas obligatorias de la propia entidad: no se puede aprobar con un resultado viejo o armado a mano, ni con un validador vacío.
  - Cada advertencia tiene que estar cubierta por un `AcceptedWarning` de alguien que sea responsable de la expedición, con una justificación no vacía. Las aceptaciones quedan registradas en la expedición (`getAcceptedWarnings`) para el informe. No se puede crear un `AcceptedWarning` sobre un resultado crítico: lo crítico no se acepta, se corrige. Una aceptación cubre una advertencia si es el mismo hallazgo (misma regla y mismo sujeto), no si el texto coincide (D32).
  - La entidad es dueña de las reglas obligatorias (D31): `approve` arma la `ApprovalPolicy` con las siete reglas fijas más las `additionalRules` que reciba. Quien llama puede sumar reglas, nunca sacar. Los tests de la entidad pasan reglas adicionales con resultados fijos para probar las advertencias y las aceptaciones; la política y cada regla se prueban por separado.
  - La transición se verifica antes de validar: una expedición que no está en revisión ni siquiera se valida.
  - Al aprobar se reservan los recursos reutilizables asignados, cada uno para el horario de su ítem. Recién ahí el plan compromete recursos: mientras es borrador se puede asignar y reasignar libremente, y las demás expediciones ven esos recursos ocupados recién cuando esta se aprueba. La reserva no la hace la expedición: la coordina el caso de uso `ApproveExpedition` (D28) con `ResourceReservations`, todo o nada (D27).
- **Alternativas descartadas:**
  - `approve(ApprovalResult)` — permitiría aprobar con un resultado calculado antes de un cambio, o con uno vacío.
  - `approve(ExpeditionValidator, ...)`, con un validador que elige quien llama (primera versión) — una inmersión sin buzos se aprobaba pasando un validador vacío (D31).
  - `Map<ValidationResult, String>` de justificaciones (boceto inicial) — no registra quién aceptó cada advertencia, y el enunciado pide que la acepte un responsable.
  - Reservar los recursos al asignarlos — la validación de superposición nunca encontraría nada, porque la segunda reserva fallaría con excepción en vez de reportarse junto con los demás problemas.
- **Consecuencia:** si algún recurso no se puede reservar (tomado por otra expedición entre la validación y la aprobación, o compartido por dos ítems propios en horarios superpuestos), `ApproveExpedition` corta con `ResourceUnavailableException` sin aprobar, sin reservar nada y sin registrar advertencias (D27, D28).


### D23 — El seguimiento es el estado real de cada ítem, y entra por la expedición

- **Patrón / principio:** encapsulamiento (misma puerta única que D19) + Information Expert.
- **Dónde:** `ActivityTracking`, `Observation` e `Incident` (en `business.models.expeditions`); `Expedition.start`, `suspend`, `resume`, `finish`, `startActivity`, `finishActivity`, `recordObservation` y `reportIncident`; `InvalidTrackingException`.
- **Por qué:** el enunciado pide registrar comienzo, finalización, observaciones, incidentes y resultados, que es el estado real de un ítem del itinerario y no lo planificado: `ItineraryItem` sabe cuándo *debía* hacerse y `ActivityTracking` cuándo se hizo, y un ítem sin tracking es una actividad que todavía no arrancó, así que el `Optional` distingue "no empezó" de "empezó y no terminó". Sólo se registra con la expedición en ejecución, y los métodos que mutan el tracking son package-private: igual que las asignaciones (D19), la expedición es la única puerta, porque la regla depende de su estado. `Observation` e `Incident` son conceptos distintos aunque tengan la misma forma, porque una observación es información y un incidente es lo que dispara una replanificación, y separarlos permite que el informe cuente incidentes sin filtrar texto libre. Al terminar una actividad se consume lo asignado, y si no alcanza la actividad no se cierra (D30): es el único momento en que el stock baja de verdad, porque antes era una previsión (D4) y la validación sólo comparaba contra el stock disponible, y así el informe puede contrastar consumo estimado contra consumo real. `startedOutOfSchedule` usa `TimePeriod.includes` para detectar una actividad que arrancó fuera de su franja, pero no lo impide: en el campo los retrasos existen, y lo que corresponde ante un desvío es replanificar, no rechazar el registro de lo que pasó.
- **Alternativas descartadas:** campos sueltos en `ItineraryItem` (`startedAt`, `finishedAt`, listas de notas) — mezclaría el plan con la ejecución en una clase que ya tiene las asignaciones, y obligaría a chequear nulos en cada lugar. Exigir que el comienzo real caiga dentro del período agendado — haría imposible registrar lo que realmente pasó, que es justamente para lo que sirve el seguimiento. Consumir los consumibles al asignarlos o al aprobar — descontaría stock de algo que todavía no ocurrió, y una expedición suspendida o replanificada dejaría el catálogo mal.
- **Revisado en D48:** los incidentes ya no viven en `ActivityTracking`: la expedición lleva una bitácora (`registerIncident`, `getIncidents`) porque un incidente puede afectar a una actividad que todavía no empezó, y cada uno tiene id, actividad afectada y, si se informan, el recurso y el retraso. `reportIncident` se reemplazó por `registerIncident`. Las observaciones siguen en el seguimiento.

### D24 — El informe es una proyección que no calcula nada por su cuenta

- **Patrón / principio:** DRY + separación entre estado y proyección; Dependency Injection por constructor.
- **Dónde:** `ExpeditionReporter` (en `business.reports`) y su resultado `ExpeditionReport` (en `business.models.report`); `ItineraryItem.supplyFor`.
- **Por qué:** el informe que pide el enunciado (resumen operativo, consumo de recursos, riesgos e incidentes) es la misma información que ya tienen la estimación (D4) y el seguimiento (D23), vista junta. Por eso el reporter recibe el `ExpeditionEstimator` en el constructor y le delega todo lo estimado en vez de recalcularlo: si cambia cómo se combina el riesgo, el informe no se entera. Lo único propio del informe es contrastar el plan con lo que pasó: el consumo real sale de las actividades terminadas y el estimado de todas, así que un itinerario a medio ejecutar muestra los dos números distintos. `ExpeditionReport` es un `record` inmutable y sin builder porque no protege ningún invariante: es una foto de datos que ya validó otro. La regla de qué consumible cubre cada requisito pasó a `ItineraryItem.supplyFor`, porque la necesitaban tres clientes distintos (la validación de consumibles, el consumo real al terminar una actividad y el informe) y repetirla habría sido la duplicación que más caro se paga: la que se desincroniza sin que ningún test la note.
- **Alternativas descartadas:** que el informe recorra el itinerario y calcule duración y riesgo por su cuenta — duplicaría las reglas de agregación de D4. Que sea `Expedition.report()` — mezclaría en la entidad una responsabilidad de presentación que cambia por motivos distintos (SRP), y obligaría a la expedición a conocer al estimador. Guardar el informe dentro de la expedición — quedaría desactualizado apenas se registre algo nuevo, cuando generarlo cuesta lo mismo que leerlo.
- **Revisado en D48:** los incidentes del informe salen de la bitácora de la expedición y no del seguimiento de cada actividad.

### D25 — Replanificación: reutilizar el builder para construir una alternativa en borrador

- **Patrón / principio:** reutilización de Builder (D3) y de las puertas ya existentes (D19, D21) en vez de una regla nueva.
- **Dónde:** `ExpeditionReplanner` (en `business.replanning`), único método `planAlternative(Expedition original, ItineraryItem affectedItem, ReplanReason reason)`. Recibe el `ExpeditionRepository` por constructor y arma la alternativa con `build(repository)`, así que el id lo da el sistema y no quien llama (D26). Lo invoca el caso de uso `ReplanExpedition`, que solo guarda la alternativa (D28). Solo replanifica una original que puede pasar a reemplazada (D33).
- **Por qué:**
  - D20 ya había decidido que una expedición aprobada no vuelve a borrador: si hay que cambiarla, se construye una alternativa. Esta clase es esa construcción: arma un `ExpeditionBuilder` con los datos generales de la original (objetivos, zonas, responsables, restricciones, permisos), mantiene como ítems cerrados los que la original ya terminó, agenda de nuevo cada ítem que no empezó, salvo el afectado, y copia sus asignaciones (D33). El resultado es una `Expedition` nueva en borrador.
  - **Revisado en D33:** en la primera versión los tres motivos (cancelación, indisponibilidad y retraso) se resolvían con el mismo primitivo, "sacar el ítem afectado", y el motivo no se guardaba. El informe señaló que los tres se tratan distinto y que, sin motivo, no queda registrado por qué existe una alternativa. Hoy el motivo es un Strategy (`ReplanReason`) que decide qué pasa con la actividad afectada, y la alternativa lo guarda.
  - Reutiliza tal cual el chequeo de dependencias de `Itinerary.add` (D21): si una actividad ya agendada depende de la afectada, construir la alternativa falla con `InvalidScheduleException`, señalando que esa dependencia también hay que resolverla antes de poder generar la alternativa.
  - La original no se toca: son dos expediciones distintas por identidad (D15), así que la original queda intacta para historial o auditoría en vez de reemplazarse.
- **Alternativas descartadas:**
  - Mutar la expedición original in situ — contradice D20.
  - Reagendar automáticamente la actividad afectada eligiendo horario y recursos — el enunciado pide "construir una alternativa" para que un responsable la ajuste, no un algoritmo de reprogramación; además duplicaría la lógica de asignación (D18) sin un criterio de selección definido por el enunciado.
- **Consecuencia:** si el ítem afectado tiene dependientes ya agendados, `planAlternative` falla en vez de dejarlos huérfanos o quitarlos en cascada; quien replanifica tiene que decidir explícitamente qué hacer con ellos primero.
- **Revisado en D46:** la consecuencia de arriba ya no vale. Cancelar una actividad cancela en cascada las que dependen de ella (directa o indirectamente), y la alternativa solo falla si alguna de esas ya empezó.


### D26 — El id de la expedición es un value object y lo genera el sistema (corrección de la Entrega 1)

- **Origen:** corrección de la Entrega 1, punto 5 (el id de la expedición lo inventa el cliente). Se construían dos expediciones con el mismo id sin ningún error. Evaluamos la sugerencia del informe (que la expedición obtenga su id de un `nextId()` en un repositorio) y la seguimos.
- **Patrón / principio:** value object (D8) + Dependency Inversion (el negocio define el contrato del repositorio y la implementación es un detalle).
- **Dónde:** `ExpeditionId`; interfaz `ExpeditionRepository` (en `business.providers`: `nextId`, `add`, `findById`) e implementación `InMemoryExpeditionRepository` (en `providers`); `DuplicateExpeditionException`; `ExpeditionBuilder.build(ExpeditionRepository)`; el caso de uso `CreateExpedition` (D28) y `ExpeditionReplanner`, que recibe el repositorio por constructor (D25).
- **Por qué:**
  - Las reservas y el stock reservado usan este id para saber de qué expedición son (D27, D30). Si el id pudiera repetirse, "liberar lo de e-1" sería ambiguo.
  - No es el mismo caso que los recursos: el id de un recurso es **natural** (una patente, un documento), viene de afuera y el catálogo garantiza que no se repita (D18). El de una expedición es **sustituto**: no significa nada fuera del sistema, así que lo genera el sistema. El builder le pide el id al repositorio al construir (`build(repository)`): `CreateExpedition` construye la expedición y la guarda, y `ExpeditionReplanner` (que tiene el repositorio por constructor) construye así la alternativa de una replanificación, que `ReplanExpedition` guarda. Ningún cliente le pone el id a una expedición. Dos `build` seguidos con el mismo repositorio dan ids distintos. `add` rechaza un id que ya existe, con el mismo criterio que el catálogo.
  - El builder ya no tiene `withId` ni un `build` que reciba un id: recibe el repositorio en `build(ExpeditionRepository)` y de ahí saca el id. Arma la expedición con los datos de negocio, pero no permite inventarle la identidad. Para tener ids predecibles, los tests usan un repositorio de prueba que los da en secuencia (`e-1`, `e-2`).
  - El repositorio es una interfaz de `business.providers` porque dónde se guardan las expediciones es un detalle (Clase 3). La implementación en memoria usa UUID, así que dos ids generados no coinciden aunque haya expediciones cargadas de otro lado.
  - Como value object, `ExpeditionId` compara por valor (D15), falla en blanco o nulo con la división de D16 y su `toString()` devuelve solo el valor.
- **Alternativas descartadas:**
  - Dejar el `String` y controlar solo la unicidad — no evita confundirlo con otro id.
  - Un `IdGenerator` aparte del repositorio — generar el id y garantizar que no se repita quedarían en lugares distintos, y la unicidad depende de lo que ya está guardado.
  - Dejar `withId` y validar la unicidad recién al guardar — el builder seguiría invitando a inventar el id.


### D27 — Reservas con dueño, liberables y todo o nada; indisponibilidades aparte (corrección de la Entrega 1)

- **Origen:** corrección de la Entrega 1, punto 1 (las reservas no tienen dueño y no se pueden liberar; tampoco se distingue una reserva de una indisponibilidad, como una licencia o un mantenimiento, y terminar antes una expedición no libera sus horarios). Evaluamos la sugerencia del informe (que la reserva tenga dueño, `Reservation(expeditionId, period)`, que se pueda liberar lo de una expedición y que chequear y reservar sea una sola operación de un servicio) y la seguimos.
- **Patrón / principio:** value object (D8) + Information Expert (el calendario es el registro de compromisos, así que es quien libera) + servicio de dominio para la operación que toca varios recursos a la vez.
- **Dónde:** `Reservation(ExpeditionId, TimePeriod)` y `Unavailability(TimePeriod, reason)` en `business.models.resources`; `AvailabilityCalendar.book(Reservation)`, `releaseAll(ExpeditionId)` y `markUnavailable(Unavailability)`; `ReusableResource.reserve(Reservation)`, `releaseAll(ExpeditionId)` y `markUnavailable(Unavailability)`; `ResourceReservations.requireReservable(Expedition)`, `reserveFor(Expedition)` y `releaseFor(Expedition)` en `business.services`, invocados desde los casos de uso `ApproveExpedition` y `FinishExpedition` (D28).
- **Por qué:**
  - Reservar es comprometer un recurso *para alguien*. El calendario guardaba `TimePeriod` sueltos, así que no podía responder de quién era una franja ni deshacerla cuando esa expedición ya no la necesitaba. Con el dueño en la reserva, `releaseAll` libera exactamente lo de una expedición y deja intacto lo de las demás. Liberar una expedición que no tiene reservas no es un error (mismo criterio que repetir una asignación, D19).
  - La regla de disponibilidad no cambia: una franja reservada no está libre para nadie, sin importar quién la reservó. La única excepción es la alternativa de una replanificación, que puede preguntar ignorando las reservas de la expedición que reemplaza (`isFreeDuring(period, ignoredOwners)`, D33).
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

- **Origen:** corrección de la Entrega 1, punto 7 (dónde viven los casos de uso) y punto 1 (el servicio de reservas, "invocado desde el caso de uso de aprobación"). Evaluamos la sugerencia del informe y la seguimos con `ApproveExpedition`, `FinishExpedition`, `CreateExpedition` y `ReplanExpedition`.
- **Patrón / principio:** caso de uso (capa de aplicación) + inyección de dependencias por constructor + composition root (`Main`, Clase 3).
- **Dónde:** en `business.usecases`: `ApproveExpedition`, que recibe las reglas adicionales de aprobación y el `ResourceReservations` en el constructor; `FinishExpedition`, que recibe el `ResourceReservations`; `CreateExpedition`, que recibe el `ExpeditionRepository`; y `ReplanExpedition`, que recibe el `ExpeditionRepository` y el `ExpeditionReplanner`.
- **Por qué:**
  - Una operación es método de la entidad cuando toca un solo objeto y no necesita nada de afuera (`schedule`, `assign*`, `startActivity`). Aprobar no es así: cambia el estado de la expedición y el calendario de cada recurso que usa, y necesita el servicio de reservas. Esa coordinación es un caso de uso.
  - El orden importa: primero `requireReservable` (verifica sin reservar), después `Expedition.approve` (valida, exige las advertencias aceptadas y pasa a aprobada) y recién al final `reserveFor`. Si falla la verificación o la validación, no cambió nada. Si se aprobara antes de verificar, una reserva fallida dejaría una expedición aprobada sin recursos. Si la expedición reemplaza a otra, `ApproveExpedition` la busca en el repositorio y, en la misma operación, libera sus reservas y la pasa a reemplazada (D33).
  - Las reglas adicionales y el servicio llegan por constructor: quien aprueba no elige con qué validar en cada llamada, y las reglas obligatorias las pone la entidad (D31).
  - Terminar también cambia la expedición y los calendarios de sus recursos. `FinishExpedition` primero termina la expedición y después libera: si la transición no es válida, falla antes de liberar nada, y liberar no puede fallar.
  - Crear una expedición necesita algo de afuera: su id, que lo da el repositorio (D26). `CreateExpedition` es el dueño de esa decisión, como pide el informe: construye con `build(repository)` y guarda. `ReplanExpedition` le pide la alternativa al replanificador (que tiene el repositorio y arma la alternativa con `build(repository)`) y solo la guarda.
- **Alternativas descartadas:** que el caso de uso reserve primero y libere si la validación falla — es la vuelta atrás que D27 ya descartó.
  - Alguien tiene que armar el grafo: `Main.wire()` (en el paquete raíz) crea el repositorio en memoria, el servicio de reservas, el replanificador y los casos de uso, y los entrega juntos en `FieldOps`. Es el único lugar que conoce las implementaciones concretas, como `InMemoryExpeditionRepository`; el negocio solo conoce la interfaz `ExpeditionRepository`. Como esta entrega no tiene interfaz de usuario, `main` solo arma el grafo.
  - El test de integración usa `Main.wire()` y recorre el ciclo a través de los casos de uso: crear, aprobar, ejecutar y terminar; y también replanificar en el campo (suspender, replanificar con un retraso, aprobar la alternativa y terminarla).
- **Consecuencia:** `Expedition.approve` y `Expedition.finish` siguen siendo públicos y no tocan los calendarios; la aprobación y la finalización completas son las de los casos de uso.
- **Nota (Entrega 2):** `Main.wire()` y `FieldOps` ya no existen: el armado del grafo pasó a la `@Configuration` `FieldOpsConfig` del módulo `web`, y el test de integración recibe los casos de uso inyectados con `@SpringBootTest` (D37). D38 suma casos de uso y entrada por id a los de esta decisión. El resto de esta decisión sigue vigente.

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
  - Reservar no es consumir: el stock real (`getStock()`) baja recién cuando se usa (D23), pero lo reservado ya no está disponible para nadie más. `hasStockFor` mira el disponible (stock menos todo lo reservado), así que el catálogo y la validación de consumibles ven lo que otras expediciones ya tienen tomado sin cambiar su código. La alternativa de una replanificación cuenta como disponible lo que reservó la expedición que reemplaza (`hasStockFor(quantity, ignoredOwners)`, D33).
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
- **Patrón / principio:** Policy, con un mínimo fijo del dominio + Open/Closed para sumar reglas + Information Expert (la entidad sabe cuándo puede aprobarse).
- **Dónde:** `ApprovalPolicy` (en `business.validations`, antes `ValidationOrchestrator`), clase `final` con las siete reglas como obligatorias y un constructor que solo recibe reglas adicionales. `Expedition.approve(additionalRules, acceptances)` la arma y la corre; `ApproveExpedition` recibe las reglas adicionales y se las pasa a la entidad (D28).
- **Por qué:**
  - "Una expedición con errores críticos no puede aprobarse" es la regla central del enunciado, y qué cuenta como crítico tiene que decidirlo el dominio, no quien llama. Las siete reglas viven dentro de la política y corren siempre; quien aprueba puede agregar reglas (por ejemplo, más estrictas para un tipo de expedición), nunca sacar.
  - La entidad es dueña de esas reglas obligatorias: `Expedition.approve` hace `new ApprovalPolicy(additionalRules).validate(this)`. Ni la entidad ni el caso de uso reciben un validador ya armado, así que no hay forma de aprobar saltándose el mínimo, se llame a `ApproveExpedition` o directo a la entidad.
  - Como la entidad aplica las siete reglas, aprobar en un test de la entidad exige un itinerario completo (personal, equipo y consumibles asignados). Para probar las advertencias y las aceptaciones sin armar todo eso, los tests pasan reglas adicionales con resultados fijos.
  - `ExpeditionValidator`, la interfaz que recibía `approve`, se eliminó: nadie más la usaba.
- **Alternativas descartadas:**
  - Que `Expedition.approve` siga recibiendo un validador (o la `ApprovalPolicy` armada por quien llama) — el mínimo quedaría en manos de quien llama, o solo garantizado si pasa por el caso de uso.
  - Que la política exponga sus reglas o permita quitar alguna — volvería a dejar la decisión en quien llama.
- **Consecuencia:** llamar directo a `Expedition.approve` también corre las siete reglas, así que no se aprueba sin ellas. Lo que sigue siendo exclusivo del caso de uso es lo que toca otros objetos: la aprobación completa, con las reservas (D28), es la de `ApproveExpedition`.

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

### D33 — El ciclo de vida de la replanificación (corrección de la Entrega 1)

- **Origen:** corrección de la Entrega 1, punto 6 (el ciclo de vida de la replanificación no estaba definido). `planAlternative` no miraba el estado de la original: aceptaba un borrador, que se puede editar directamente, y una expedición en ejecución. El informe propone un camino y pide elegir uno y documentarlo; seguimos el suyo. También responde la pregunta abierta sobre el motivo de la replanificación ("una cancelación libera y no reagenda, una indisponibilidad cambia un recurso y un retraso corre horarios; además, sin motivo no queda registrado por qué existe una alternativa").
- **Patrón / principio:** la regla vive en la tabla de transiciones (D20) + caso de uso para la operación (D28) + Strategy para el motivo (mismo criterio que `ActivityRules`, D1).
- **Dónde:** estado `REPLACED` en `ExpeditionStatus` (aprobada → reemplazada, suspendida → reemplazada, reemplazada → ninguna); `Expedition.requireReplaceable()`; `ExpeditionReplanner.planAlternative`; caso de uso `ReplanExpedition`; `ExpeditionBuilder.addFinishedItem` e `Itinerary.pendingItems()`; `ExpeditionBuilder.withReplacedExpedition`, `Expedition.getReplacedExpedition()`, `getReplacement()`, `reservationsItTakesOver()` y `markReplacedBy()`; `ApproveExpedition` cuando la expedición reemplaza a otra; interfaz `ReplanReason` (en `business.interfaces.replanning`) y su respuesta `ReplannedActivity`, con `Cancellation`, `Delay` y `ResourceWithdrawn` (en `business.replanning`), `Expedition.getReplanReason()` y `TimePeriod.shiftedBy`.
- **Por qué:**
  - Solo se replanifica una expedición aprobada o suspendida. Un borrador se corrige directamente, y una en ejecución primero se suspende. La regla es "se puede replanificar si la expedición puede pasar a reemplazada", así que vive en la misma tabla de transiciones que los demás estados y no en un `if` aparte.
  - Tampoco se replanifica con actividades en curso: primero se cierran, igual que para terminar (D29). Si no, la actividad en curso no quedaría ni en la original ni en la alternativa.
  - `ExpeditionReplanner` arma la alternativa con `build(repository)` y `ReplanExpedition` la guarda (D26), así que el id de la alternativa tampoco lo inventa quien llama.
  - El nuevo plan es la expedición completa: lo que ya pasó se mantiene. Los ítems que la original ya terminó pasan a la alternativa como ítems cerrados, con su horario, sus asignaciones y su seguimiento (inicio, fin, resultado, observaciones e incidentes). Así el informe de la alternativa muestra la salida entera, y una actividad pendiente que dependía de una ya hecha se agenda sin problemas.
  - Lo único que se puede cambiar es lo pendiente. Un ítem que ya ocurrió no se reasigna, y la misma actividad no se puede volver a agendar. Lo ya hecho tampoco se vuelve a reservar, a pedir de stock ni a validar como riesgo: la reserva de recursos, la demanda de consumibles, la superposición y la tolerancia de riesgo miran solo `pendingItems()`. Esto cubre lo que el informe quería evitar ("volvería a agendar actividades hechas y a pedir sus insumos") manteniendo lo hecho en el plan.
  - Las dos expediciones quedan vinculadas en los dos sentidos: la alternativa sabe a cuál reemplaza (`getReplacedExpedition`, lo pone el replanificador) y la original sabe por cuál fue reemplazada (`getReplacement`, se registra al aprobar la alternativa). Un estado reemplazada sin saber por cuál dejaría la mitad de la historia.
  - La alternativa pasa por revisión como cualquier expedición, pero no compite con las reservas de la que reemplaza: la validación de superposición, la de consumibles y el servicio de reservas ignoran las reservas de esa expedición (`reservationsItTakesOver`). Una indisponibilidad no se ignora nunca.
  - Aprobar la alternativa es una sola operación de `ApproveExpedition`: verifica que la original todavía se pueda reemplazar y que la alternativa se pueda reservar, aprueba la alternativa, libera las reservas de la original, reserva las de la alternativa y pasa la original a reemplazada. Si falla la verificación o la validación, no cambió nada. Liberar recién al aprobar evita que otra expedición tome esos recursos mientras la alternativa está en revisión, y que la original quede sin recursos si la alternativa nunca se aprueba: si se descarta, la original sigue como estaba.
  - Desde reemplazada no se pasa a ningún otro estado: lo que quedaba por hacer lo hace la alternativa, y la original queda como historial de lo que sí llegó a hacer.
  - Cada replanificación tiene un motivo, y cada motivo trata distinto a la actividad afectada. `ReplanReason` responde una sola pregunta, `replan(actividad afectada)`: vacío si no vuelve a la alternativa, o un `ReplannedActivity` con el horario en que vuelve y qué recursos conserva. Una **cancelación** (`Cancellation`) no la vuelve a agendar. Un **retraso** (`Delay`) la agenda corrida por esa duración con los mismos recursos; si así queda fuera de su ventana, la alternativa no se puede armar (D21). Un **recurso retirado** (`ResourceWithdrawn`) la mantiene en su horario sin el recurso que falta, y la validación lo reporta como faltante para que el responsable asigne otro. Este motivo NO registra una `Unavailability` en el calendario del recurso: eso lo hace quien conoce el motivo (licencia, mantenimiento) con `markUnavailable`. Agregar un motivo es agregar una clase.
  - La alternativa guarda su motivo (`getReplanReason`): queda registrado por qué existe.
  - La actividad afectada tiene que estar pendiente: lo que ya pasó no se replanifica.
- **Alternativas descartadas:**
  - Una lista de estados permitidos dentro del replanificador — repetiría en otro lugar una regla que ya expresa la tabla de transiciones.
  - Pasar solo los ítems que no empezaron, como sugiere el informe al pie de la letra — el plan nuevo quedaría incompleto: lo que ya pasó desaparecería de él, y una actividad pendiente que dependía de una ya hecha no se podría agendar.
  - Copiar los ítems terminados como si estuvieran pendientes — volvería a ejecutarlos y a pedir sus insumos.
  - Liberar las reservas de la original al empezar a replanificar — otra expedición podría tomarlas mientras la alternativa está en revisión, y la original quedaría sin recursos si la alternativa nunca se aprueba.
  - Reusar `FINISHED` para la original — contaría como ejecutada una expedición que no se terminó, y se perdería que fue reemplazada.
  - Un único primitivo para los tres motivos ("sacar la actividad afectada") y que el responsable rearme a mano — era la primera versión (D25): obligaba a repetir a mano lo que cada motivo ya define y no dejaba registrado por qué existe la alternativa.
  - Un `enum` de motivos con un `switch` en el replanificador — cada motivo nuevo obligaría a modificarlo (Open/Closed).
  - Dos métodos separados en `ReplanReason` (el horario y qué recursos conserva) — una cancelación tendría que implementar uno que nunca se usa, porque la actividad no vuelve (Interface Segregation). Con una sola pregunta, cada motivo responde exactamente lo que necesita.
  - Elegir automáticamente el reemplazo del recurso indisponible — haría falta un criterio de selección que el enunciado no define; el catálogo ya ofrece candidatos (D18).
- **Revisado en D39:** la alternativa conserva los bloques de la original. El replanificador ya no pasa los terminados con `ExpeditionBuilder.addFinishedItem` y el resto con `addActivity` ordenados por inicio: le pide al itinerario un plan con su misma estructura (`Itinerary.planKeepingStructure`) y decide por actividad (la terminada va como `PlannedNode.finished`, la afectada como diga el motivo, el resto con su horario). La alternativa se agenda en el orden en que se agendó la original. `addFinishedItem` sigue existiendo como atajo; `addFinishedCopyOf` ya no.
- **Revisado en D46 y D47:** la actividad afectada ya no tiene que estar pendiente. Si había empezado (un incidente sobre una actividad en curso, que se cierra antes de aceptar), la alternativa la rehace cuando el motivo la devuelve y la deja cerrada cuando la cancela. El replanificador saca las consecuencias del motivo para el resto del itinerario: lo que vuelve más tarde corre lo que viene después en sus secuenciales y lo que depende de ella; lo que no vuelve se lleva lo que depende de ella. Hay un motivo más, `ResourceReplaced` (reemplazo elegido entre candidatos del catálogo), y `canPlan` responde si una alternativa se puede armar sin mirar el estado de la original. El reemplazo automático sigue descartado: las propuestas ofrecen candidatos y decide un responsable. La alternativa hereda la bitácora de incidentes de la original (D48).

### D34 — Las categorías y las certificaciones se normalizan al construirse (corrección de la Entrega 1)

- **Origen:** pregunta abierta 3 del informe (categorías y certificaciones como texto libre). Con el nombre tal cual lo escribe quien carga el dato, "Boat" y "boat " eran categorías distintas y una persona cargada con "Diving" no cumplía una actividad que pide "diving".
- **Patrón / principio:** value object (D8) que se normaliza al construirse; la igualdad por valor (D15) compara el nombre ya normalizado.
- **Dónde:** `ResourceCategory` y `Certification` (en `business.models.resources`): el constructor compacto valida el texto (D16) y guarda el nombre con `strip()` y `toLowerCase(Locale.ROOT)`. `name()` devuelve el nombre normalizado.
- **Por qué:**
  - Las dos clases son claves de comparación: el requisito de una actividad (D12, D17) se cumple si el recurso tiene la misma categoría o certificación (D10). Que la diferencia entre mayúsculas, minúsculas o un espacio de más rompa esa comparación es un error de carga, no una diferencia de negocio.
  - Normalizar en el constructor lo deja en un solo lugar: todo lo que compare categorías o certificaciones (el catálogo, las reglas de validación, `Certifications.includes`) recibe el valor ya normalizado, y ninguna tiene que acordarse de comparar sin distinguir mayúsculas. `Locale.ROOT` evita que el resultado dependa del idioma de la máquina.
  - Los mensajes de las validaciones muestran el nombre normalizado ("0 of the 1 cooler it needs"), porque es el único que guarda el value object.
- **Alternativas descartadas:**
  - Un catálogo de categorías validado al dar de alta un recurso — las categorías las definen las reglas de actividad en el código y el enunciado no pide administrarlas; un catálogo sumaría una entidad y una operación de alta que nadie pide.
  - Fecha de otorgamiento y vencimiento de las certificaciones — ninguna regla la usa hoy.
- **Consecuencia:** un error de tipeo o un sinónimo ("bote" en vez de "boat") sigue siendo otra categoría y se reporta como "falta equipo". Si el catálogo crece, la salida es un catálogo de categorías en `ResourceCatalog` y `Certification(nombre, vencimiento)`.

### D35 — Arquitectura hexagonal: puertos y adaptadores (Entrega 2)

- **Origen:** la Entrega 2 pide organizar el código según la arquitectura hexagonal (dominio, casos de uso, infraestructura y API REST). La Entrega 1 ya tenía la separación en paquetes (`business`, `providers`, `Main`); esta decisión la nombra y la hace cumplir.
- **Patrón / principio:** arquitectura hexagonal (puertos y adaptadores) + inversión de dependencias: el negocio define las interfaces que necesita y la infraestructura las implementa.
- **Dónde:** en el código de la Entrega 1 no se renombró nada; cambia el rol que se le da a cada paquete:

  | Entrega 1 | Entrega 2 |
  |---|---|
  | `business` (modelo, reglas, validaciones, servicios) | Hexágono (módulo `domain`) |
  | `business.providers` (`ExpeditionRepository`) | Puertos de salida |
  | `business.usecases` | Puertos de entrada |
  | `providers/InMemoryExpeditionRepository` | Adaptador driven, `adapters.driven` (módulo `app`) |
  | `Main.wire()` | `@Configuration` `FieldOpsConfig` (módulo `web`, D37) |

- **Por qué:**
  - Los paquetes de la Entrega 1 ya apuntaban en esa dirección: el negocio solo conoce la interfaz del repositorio (D28) y nada de `business` importa una implementación. Solo faltaba darle nombre a cada pieza para que los adaptadores nuevos (REST, persistencia) se ubiquen sin discutirlo.
  - Se conservaron los nombres de paquete (`business.*`) para que el trabajo en paralelo sobre el dominio no genere conflictos de rutas al integrarse.
- **Alternativas descartadas:**
  - Clean Architecture — sumaría anillos (entidades, casos de uso, adaptadores, frameworks) que para este tamaño de dominio no se distinguen del hexágono; ver la tabla de patrones no aplicados.
  - DDD táctico completo (agregados, eventos, módulos por contexto) — el dominio es un solo contexto y los conceptos que sirven ya están (entidades, value objects, servicios); ver la tabla de patrones no aplicados.
  - Separar la interfaz de cada caso de uso (puerto de entrada) de su implementación — en este PR los casos de uso siguen siendo clases concretas en `domain`. Una interfaz por caso de uso con una sola implementación no agrega nada hoy; el adaptador REST depende de la clase.
- **Consecuencia:** si aparece un segundo adaptador driving que necesite sustituir un caso de uso (por ejemplo para mockearlo), la salida es extraer la interfaz de ese caso de uso y mover la implementación a `app`.

### D36 — Multi-módulo Maven para que el compilador haga cumplir la dependencia (Entrega 2)

- **Origen:** una arquitectura hexagonal solo vale si la dependencia hacia adentro no se puede romper por descuido. Con paquetes de un mismo módulo, cualquier clase puede importar cualquier otra.
- **Patrón / principio:** módulos como fronteras (Dependency Rule): las dependencias apuntan siempre hacia el dominio.
- **Dónde:** pom raíz padre con tres módulos: `domain` (todo `business.*`, sin frameworks), `app` (`adapters.driven`, depende de `domain`) y `web` (Spring Boot, depende de `domain` y `app`). `domain` publica un `test-jar` con los fixtures (`ExpeditionFixtures`, `SequentialExpeditionRepository`) que reutilizan `app` y `web`. El pom de `domain` declara la regla `bannedDependencies` del enforcer, que prohíbe `org.springframework*` de forma directa y transitiva.
- **Por qué:**
  - Si `domain` importa una clase de `app` o `web`, no compila: no hace falta recordar la regla, la hace cumplir el compilador. Agregar Spring al dominio por error (directo o arrastrado por otra dependencia) rompe el build por la regla del enforcer.
  - Los fixtures de test se comparten como `test-jar` en vez de duplicarse.
  - Para evitar un ciclo `domain` <-> `app`, tres tests de `domain` que usaban `InMemoryExpeditionRepository` (ahora en `app`) pasaron a usar `SequentialExpeditionRepository`; es solo el tipo del repositorio de test, no cambia lo que prueban.
- **Alternativas descartadas:**
  - Un solo módulo con paquetes y un test de arquitectura (ArchUnit) — la regla se verifica recién al correr los tests y se puede olvidar o debilitar; en módulos, romperla es un error de compilación.
- **Consecuencia:** hay tres poms para mantener y los cambios entre capas pasan por los límites de módulo. A cambio, sumar un adaptador nuevo (por ejemplo persistencia JPA) es sumar un módulo sin tocar `domain`.

### D37 — Spring solo en `web`; la `@Configuration` es el composition root (Entrega 2)

- **Origen:** la Entrega 2 usa Spring Boot para la API REST. D28 había dejado el armado del grafo en `Main.wire()`, único lugar que conocía las implementaciones concretas.
- **Patrón / principio:** composition root (Clase 3) + inyección de dependencias; el contenedor de Spring es un detalle de infraestructura, no del negocio.
- **Dónde:** en el módulo `web`: `FieldOpsApplication` (`@SpringBootApplication`) y `FieldOpsConfig` (`@Configuration`), que expone como beans el `ExpeditionRepository` (`InMemoryExpeditionRepository`), un `ResourceReservations` compartido, los casos de uso `CreateExpedition`, `ApproveExpedition` (con `List.of()` de reglas adicionales), `ReplanExpedition` (con `ExpeditionReplanner`) y `FinishExpedition`, y el `ExpeditionReporter`. Se eliminaron `Main` y `FieldOps` (que solo agrupaba los objetos para el `main`). El BOM `spring-boot-dependencies` se importa solo en el pom de `web`.
- **Por qué:**
  - Los casos de uso reciben todo por constructor, así que el dominio no necesita anotaciones: `FieldOpsConfig` hace con `@Bean` lo mismo que hacía `wire()` y las clases de `business` no cambian.
  - Spring queda confinado a `web`: `domain` y `app` no lo traen, y el enforcer lo verifica (D36).
  - El test de integración recibe los casos de uso inyectados con `@SpringBootTest` en lugar de llamar a `Main.wire()`; el comportamiento que prueba es el mismo. Como el repositorio y las reservas son beans compartidos, cada test levanta un contexto nuevo (`@DirtiesContext`) para no heredar estado, equivalente a llamar a `wire()` en cada test.
- **Alternativas descartadas:**
  - Anotar las clases de `business` con `@Service` / `@Component` — acoplaría el hexágono al framework y rompería D36.
  - Mantener `Main.wire()` y exponer sus resultados como beans — dos composition roots que mantener sincronizados.
- **Consecuencia:** cada área puede aportar su propia `@Configuration` en `web` sin tocar las demás. Los beans son singletons, por lo que el estado en memoria se comparte entre pedidos hasta que haya persistencia.

### D38 — Casos de uso con comandos por id y consultas (Entrega 2, amplía D28)

- **Origen:** el adaptador REST recibe ids y no debería armar objetos de dominio ni cargar expediciones por su cuenta. En la Entrega 1 solo cuatro operaciones eran casos de uso y recibían la `Expedition` o un `ExpeditionBuilder` ya armados; el resto (agendar, asignar, cambiar de estado, seguir) se llamaba directo sobre la entidad.
- **Patrón / principio:** caso de uso con comando (`record`) o ids como entrada (D28), repositorio como puerto de salida (D35) e Information Expert: los casos de uso resuelven los ids y la entidad sigue aplicando las reglas.
- **Dónde:** en `business.usecases`:
  - Comandos: `CreateExpeditionCommand` (zonas y responsables por id), `ScheduleActivityCommand` y `WarningAcceptance` (regla, sujeto, responsable por id y justificación).
  - Operaciones: `ScheduleActivity`, `AssignResources` (personal, equipo y consumible), `TransitionExpedition` (enviar a revisión, devolver a borrador, iniciar, suspender, reanudar) y `TrackActivity` (iniciar, finalizar y observar).
  - Consultas: `QueryExpeditions`, `ValidateExpedition` (corre las reglas de aprobación sin aprobar), `EstimateExpedition`, `ReportExpedition` y `FindAssignmentCandidates`, que devuelve para cada requisito de una actividad los candidatos del catálogo.
  - Los casos de uso de D28 suman una variante por id: `ApproveExpedition.approve(id, aceptaciones)`, `FinishExpedition.finish(id)` y `ReplanExpedition.replan(id, actividad, motivo)`. Las variantes que reciben el objeto siguen existiendo.
  - Puertos y búsquedas que hacían falta: `ExpeditionRepository.findAll()`, `ZoneRepository` (con `InMemoryZoneRepository` en `app`), búsqueda por id en `ResourceCatalog` (`findStaff`, `findEquipment`, `findSupply`) y `Itinerary.itemFor(ActivityId)`. `FieldOpsConfig` expone un único `ResourceCatalog` compartido.
  - `NotFoundException` para un id que no existe, y una clase auxiliar privada del paquete (`Lookups`) que resuelve los ids.
- **Por qué:**
  - Los casos de uso son la frontera: el adaptador REST solo traduce el pedido a ids o a un comando, y todo lo que pasa después ocurre dentro del hexágono.
  - Una aceptación de advertencia por id se arma contra las advertencias que la expedición levanta en ese momento. Si no existe esa advertencia, falla con `NotFoundException` en vez de ignorarse: una aceptación vieja no debería quedar como si hubiera cubierto algo.
  - Cada caso de uso depende de los puertos que usa (D35). Los tests unitarios usan Mockito para el repositorio y entidades reales.
  - Los repositorios tienen un test de contrato (`ExpeditionRepositoryContract`, `ZoneRepositoryContract`) que hoy corre contra el adaptador en memoria y después correrá contra JPA.
- **Alternativas descartadas:**
  - Una clase por transición de estado — cinco clases de una línea cada una; `TransitionExpedition` las agrupa porque todas hacen lo mismo (buscar y llamar a la entidad).
  - Que el adaptador REST busque la expedición y llame a la entidad — duplicaría en cada adaptador la resolución de ids y los errores de "no existe".
  - Reemplazar las variantes que reciben el objeto — rompería los tests y el uso de la Entrega 1 sin necesidad.
  - Un método `save` en el repositorio — con el repositorio en memoria las entidades se modifican en el lugar. Cuando haya persistencia real hará falta.
- **Consecuencia:**
  - ~~`ScheduleActivityCommand` recibe la `Activity` ya armada: crearla requiere elegir su `ActivityRules` por tipo y parámetros, y eso lo resuelve el registro de tipos de la Fase 3. Hasta entonces el adaptador que agenda una actividad la construye.~~ **Resuelto en D53:** el comando recibe el id de la actividad, su nombre, su ventana, el horario, el id de la zona, los ids de las dependencias, el nombre del tipo, sus parámetros y el indicador de nocturna, y `ScheduleActivity` resuelve las tres cosas (zona, dependencias y reglas). Ningún adaptador arma una `Activity`.
  - `ValidateExpedition` y `ApproveExpedition` reciben cada uno su lista de reglas adicionales; hoy ambas están vacías en `FieldOpsConfig`.
  - Los tests del repositorio se podrán reutilizar contra el adaptador JPA cuando exista.
- **Ampliado en D52:** el alta del catálogo y de las zonas, que no tenía caso de uso, lo tiene con el mismo esquema (`RegisterResources`, `QueryCatalog`, `MarkResourceUnavailable`, `RegisterZone`, `QueryZones`). `Lookups` resuelve además la zona por id, el equipo por tipo y el consumible.
- **Revisado en D53:** el comando de agendar una actividad pasa a recibir ids, tipo y parámetros, que es el pendiente que esta entrada tenía anotado. La variante que recibe la `Activity` armada se conserva, como las de esta misma decisión.

### D39 — El itinerario es un árbol de actividades y bloques (Composite) (Entrega 2, F2)

- **Origen:** F2 pide que el itinerario admita bloques: agrupaciones de actividades o de otros bloques que se ejecutan en secuencia o en paralelo, y que un bloque participe de la validación, la estimación y los informes como cualquier actividad. El itinerario era una lista plana de ítems (D6) y la estimación sumaba todas las duraciones (D4), así que un bloque paralelo se habría contado de más.
- **Patrón / principio:** Composite + Open/Closed. La declaración de lo que se quiere agendar es un objeto aparte del nodo agendado (mismo criterio que D3).
- **Dónde:** interfaz `ItineraryNode` (en `business.interfaces.itinerary`). En `business.models.expeditions`: `ItineraryItem` es la hoja; `SequentialBlock` y `ParallelBlock` son los compuestos, sobre la clase abstracta `ItineraryBlock`; `PlannedNode` declara lo que se agenda. `Itinerary.getNodes()`, `getItems()` y `planKeepingStructure`; `Expedition.schedule(PlannedNode)`; `ExpeditionBuilder.addNode`; `ExpeditionEstimator`; `ExpeditionReplanner`; caso de uso `ScheduleBlock` con `ScheduleBlockCommand` (en `business.usecases`).
- **Por qué:**
  - `ItineraryNode` responde lo mismo para una actividad agendada y para un bloque: período agendado, duración estimada, riesgo, consumibles que requiere, si terminó y cuáles son sus hojas. La hoja delega en su `Activity`, como antes (D1). El bloque combina a sus hijos: la duración es la suma en `SequentialBlock` y el máximo en `ParallelBlock`, el riesgo es el más alto, el consumo se suma y está terminado cuando lo están todos. Quien recorre el itinerario no pregunta de qué tipo es cada nodo: no hay `instanceof` ni `switch`, ni en el dominio ni en los tests.
  - Son dos clases y no una con un modo porque entre secuencial y paralelo cambia comportamiento: cómo se combina la duración, qué se le exige a los hijos (D40) y si las ramas se consideran simultáneas (D41). Lo que comparten (guardar los hijos, período, riesgo, consumo, hojas) está en `ItineraryBlock`, con el mismo esquema que `ScheduledResource` (D9): la interfaz es el contrato y la clase abstracta, la implementación compartida.
  - El período de un bloque no se guarda: se calcula desde el inicio más temprano hasta el fin más tardío de sus hijos. Así no puede quedar desactualizado respecto de ellos.
  - `Itinerary` guarda los nodos raíz. `getItems()` sigue devolviendo las hojas ordenadas por inicio, así que las validaciones, las reservas, el seguimiento y el informe no cambiaron: un itinerario sin bloques es un árbol donde cada actividad es su propia raíz, y valida, estima e informa igual que antes.
  - `ExpeditionEstimator` agrega sobre las raíces en vez de sobre las actividades (D4). El informe no cambió: toma la estimación del estimador (D24), así que refleja la duración de cada bloque sin calcular nada propio.
  - `PlannedNode` existe por D3: un `ItineraryItem` no puede existir fuera de una expedición, porque es la expedición la que verifica zona, período, ventana y dependencias (D19, D21). Para declarar un árbol antes de agendarlo hace falta una descripción que todavía no sea un ítem: `PlannedNode.activity(actividad, horario)`, `sequential(...)` y `parallel(...)`. `Expedition.schedule(PlannedNode)` la convierte en nodos aplicando a cada actividad las mismas reglas que a una suelta, y los mutadores siguen siendo package-private (D19). Cada fábrica guarda el constructor de su compuesto (`SequentialBlock::new`, `ParallelBlock::new`), así que tampoco ahí hay un modo que consultar.
  - `PlannedNode.finished(ítem)` extiende a los bloques la excepción de D3 para lo ya ocurrido: un terminado tiene que poder quedar dentro de su bloque. Por eso el builder dejó de tener una lista aparte de terminados y agenda todo en el orden en que se le agrega.
  - Para replanificar, `Itinerary.planKeepingStructure` devuelve un `PlannedNode` por raíz con la misma forma: la hoja pregunta por su plan a la función que recibe, y el bloque arma el de sus hijos y se vuelve a envolver en su mismo tipo. `ExpeditionReplanner` solo decide qué pasa con cada actividad (D33); un bloque que se queda sin hijos desaparece. Los planes salen en el orden en que se agendaron las raíces y no por horario, porque una actividad dentro de un bloque puede depender de una actividad suelta que empieza después que el bloque: ordenar por inicio la agendaría antes que su dependencia.
  - `ScheduleBlock` es el caso de uso por id (D38): busca la expedición y le pasa el `PlannedNode` del comando, que ya trae las actividades armadas igual que `ScheduleActivityCommand`.
- **Alternativas descartadas:**
  - Un único bloque con un `enum` de modo y condicionales — ver la tabla de patrones no aplicados.
  - El bloque como subtipo de `Activity` — una actividad tiene ventana, zona, dependencias y reglas de tipo (D1, D5); un bloque no tiene nada de eso propio, así que heredaría campos sin sentido y las validaciones por actividad lo tratarían como una actividad sin recursos.
  - Lista plana con un id de grupo en cada ítem — no expresa anidamiento sin agregar un padre a cada grupo, y cada cliente (estimación, validación, replanificación) tendría que reconstruir el árbol para calcular.
  - Construir los bloques fuera de la expedición y pasárselos ya armados — existirían ítems y bloques que ninguna expedición verificó, y habría que repetir afuera los chequeos de zona, período y dependencias. Es lo mismo que D3 descartó para el builder.
  - Guardar el período del bloque — habría que mantenerlo consistente con los hijos y validar que los envuelva.
  - Visitor para recorrer el árbol — ver la tabla de patrones no aplicados.
- **Consecuencia:**
  - Agregar otra forma de combinar es agregar una clase que extienda `ItineraryBlock` y una fábrica en `PlannedNode`.
  - `ItineraryNode` tiene dos métodos que una hoja responde de forma trivial (`runsInParallel` y `planKeepingStructure`). Es el costo conocido de un Composite uniforme, y se acepta a cambio de no preguntar por el tipo.
  - `getItems()` se calcula en cada llamada y devuelve una copia (D6).
  - La interfaz no expone los hijos de un nodo: ningún cliente del dominio los necesita. Cómo se muestra el árbol hacia afuera queda para el endpoint.
- **Revisado en D54:** el endpoint del itinerario necesitó una operación nueva sobre el árbol (serializarlo), y se agregó `accept(ItineraryNodeVisitor<T>)` a la interfaz en vez de agregar hijos y un nombre de tipo. La invariante de este párrafo se mantiene para el dominio: ningún cliente que no sea un visitor ve la estructura.

### D40 — Qué se rechaza al armar un bloque y qué detecta la validación (Entrega 2, F2, extiende D21)

- **Origen:** D21 separó lo que se rechaza al agendar (un plan mal formado) de lo que detecta la validación (lo que depende de asignaciones y del catálogo). Con bloques hay que decidir de qué lado cae cada regla nueva.
- **Patrón / principio:** fail fast para planes mal formados, con cada invariante en el objeto que lo puede garantizar (mismo criterio que D21).
- **Dónde:** constructores de `ItineraryBlock` y `SequentialBlock`; `Expedition.schedule(PlannedNode)`; `Itinerary.add`. Todos lanzan `InvalidScheduleException`.
- **Por qué:**
  - Se rechaza al agendar, sin importar qué recursos haya: un bloque vacío; un secuencial donde un hijo empieza antes de que termine el anterior; una actividad del bloque fuera de las zonas o del período de la expedición, o fuera de su ventana; una actividad que ya está agendada o que aparece dos veces en el bloque; una dependencia sin agendar o que termina después; y una dependencia entre ramas paralelas.
  - En un secuencial, "cada hijo empieza cuando terminó el anterior" significa que no empieza antes, con huecos permitidos. Es la misma comparación que usan las dependencias (D21), con el borde compartido válido (D8): un plan real tiene traslados y esperas entre una actividad y la siguiente.
  - El orden de un secuencial es el declarado y no se reordena por horario: si se declara `[A, B]` y B está agendada antes que A, el bloque se rechaza. Un hijo que es un bloque ocupa su lugar con su período completo.
  - Un paralelo no exige nada a los horarios de sus ramas: pueden pisarse o no.
  - El período del bloque queda dentro del de la expedición porque cada hoja se verifica contra ese período y el del bloque se calcula a partir de las hojas (D39). No hace falta un chequeo propio.
  - Las dependencias siguen siendo entre actividades. Como el bloque entra entero, una dependencia puede estar agendada antes o dentro del mismo bloque, y se le aplica la misma regla de D21.
  - Una actividad no puede depender de otra que está en otra rama del mismo paralelo, aunque los horarios den: el bloque declara que las ramas pueden correr a la vez y la dependencia exige que una termine antes. Es el mismo criterio que D41 aplica a los recursos.
  - Un bloque se agenda todo o nada: el árbol se arma y se verifica antes de tocar el itinerario, así que un bloque rechazado no deja nada a medias.
  - Un bloque con un solo hijo es válido. Es un caso degenerado (suma y máximo coinciden), pero es lo que queda cuando una replanificación cancela la otra rama.
  - Agendar un bloque exige la expedición en borrador, igual que una actividad (D19).
  - Queda para la validación lo que depende de las asignaciones: el conflicto de recursos entre ramas paralelas (D41).
  - El seguimiento no exige el orden del bloque: registra lo que pasó en el campo, igual que con el horario agendado (D23).
- **Alternativas descartadas:**
  - Exigir que cada hijo de un secuencial empiece exactamente cuando termina el anterior — no dejaría lugar a traslados ni esperas, y sería más estricto que la regla de dependencias.
  - Ordenar los hijos de un secuencial por horario — aceptaría una secuencia distinta de la declarada sin avisar.
  - Dependencias implícitas entre los hijos de un secuencial — cancelar un hijo obligaría a cancelar los siguientes, y el orden ya lo garantiza el invariante.
  - Exigir al menos dos hijos — cancelar una rama de un bloque de dos obligaría a desarmarlo.
  - Reportar estos problemas como reglas de validación — permitiría armar bloques imposibles que recién se detectarían al pedir la aprobación (mismo argumento que D21).
- **Consecuencia:** al replanificar, un retraso que deja a la actividad afectada pisando al hermano siguiente de su secuencial no se puede armar y falla con `InvalidScheduleException`, igual que un retraso que la saca de su ventana (D33). Queda como deuda técnica de F2.
- **Revisado en D46:** la deuda quedó resuelta. Un retraso corre por el mismo tiempo lo que viene después de la actividad en cualquier secuencial que la contenga y lo que depende de ella. Solo falla si algo de eso ya empezó o queda fuera de su ventana o del período.

### D41 — El conflicto de recursos entre ramas paralelas vive en la regla de superposición (Entrega 2, F2)

- **Origen:** criterio de aceptación de F2: la validación detecta conflictos de recursos entre actividades paralelas del mismo bloque. `ResourceOverlapValidation` ya detectaba un recurso reutilizable asignado a dos actividades cuyos horarios se pisan (D2).
- **Patrón / principio:** Information Expert (el bloque paralelo sabe qué ramas declaró simultáneas) + una regla por concepto, con un resultado por hallazgo (D2, D32).
- **Dónde:** `ItineraryNode.runsInParallel`, que `ParallelBlock` responde por sus ramas; `Itinerary.runInParallel`; `ResourceOverlapValidation`.
- **Por qué:**
  - Dos actividades corren en paralelo cuando están en ramas distintas de un mismo `ParallelBlock`, es decir, cuando el bloque común más cercano es un paralelo. Dos actividades de una misma rama secuencial no lo están, aunque esa rama sea parte de un paralelo.
  - Dos ramas paralelas no pueden compartir un recurso reutilizable aunque sus horarios no se pisen. El bloque declara que pueden ejecutarse a la vez; si solo contara el horario, un bloque paralelo no cambiaría nada para la validación, y un retraso en una rama las haría chocar en el campo. Si dos actividades tienen que compartir el recurso, van en secuencia.
  - La regla es la misma de siempre, "un recurso no puede estar en dos lugares a la vez", con una definición más amplia de "a la vez": los horarios se pisan o el itinerario las declara paralelas. Por eso se amplió `ResourceOverlapValidation` en lugar de sumar una regla. No hubo que tocar `ApprovalPolicy`.
  - Cada conflicto se reporta una sola vez: la regla recorre cada par de actividades pendientes una vez y emite un resultado por recurso compartido, con el mismo sujeto de antes (actividad y recurso, D32). Si los horarios se pisan, el mensaje es el de siempre; si solo las une el bloque, dice que corren en paralelo.
  - Es crítico, como cualquier superposición de recursos (D2).
  - Los consumibles no entran: no se reservan por horario, y lo que piden las ramas se suma (D30).
  - `ResourceReservations` no cambió: reserva por franja horaria, y un plan con este conflicto no llega a reservarse porque la validación impide aprobarlo (D22, D28).
- **Alternativas descartadas:**
  - Una regla nueva en `ApprovalPolicy` — las actividades paralelas normalmente se pisan en horario, y ese caso ya lo reporta la regla de superposición: habría dos resultados con dos `RuleId` para el mismo par y recurso, o una de las dos reglas tendría que saber qué reporta la otra para callarse.
  - Permitir que dos ramas compartan un recurso si sus horarios no se pisan — deja al bloque paralelo sin efecto en la validación.
  - Rechazarlo al asignar el recurso — las asignaciones cambian mientras se planifica y un borrador puede estar incompleto a propósito (D21, D22).
  - Detectarlo al reservar — la reserva corta en el primer problema con una excepción en vez de reportarlo junto con los demás (D22).
- **Consecuencia:** saber si dos actividades corren en paralelo recorre el árbol por cada par. Con el tamaño de un itinerario no pesa; si pesara, el itinerario podría calcular una vez los pares paralelos.

### D42 — La actividad nocturna es un Decorator de las reglas de cualquier tipo (Entrega 2, F1)

- **Origen:** F1 pide una variante nocturna con requisitos propios: certificación de operación nocturna para todo el personal asignado, permiso especial por zona, equipamiento de iluminación y un factor de riesgo adicional. Los informes tienen que incluirla sin condicionales nuevos por tipo. Cualquier tipo de actividad puede hacerse de noche, y los tipos ya eran un Strategy (D1).
- **Patrón / principio:** Decorator sobre el Strategy de D1 + Open/Closed.
- **Dónde:** en `business.rules`: `ActivityRulesDecorator` (abstracta) y `NightOperationRules`. `RiskLevel.EXTREME` y `RiskLevel.next()`.
- **Por qué:**
  - "Nocturna" no es un tipo más: es algo que se le suma a un tipo. `NightOperationRules` implementa `ActivityRules` y envuelve otra `ActivityRules`: le pregunta a la regla envuelta y le agrega lo propio. Un buceo nocturno es `new NightOperationRules(new DivingRules(30))`, y lo mismo vale para los otros cinco tipos y para los que se agreguen.
  - Lo que suma: un `ReusableRequirement` de categoría `Lighting` al equipamiento, la certificación `Night operations` para todo el personal asignado y el permiso especial de la zona (D43), y un nivel más de riesgo. La duración, los consumibles y los requisitos de personal se delegan sin cambios.
  - `Activity` sigue delegando en sus reglas sin saber que están envueltas (D1). Las validaciones, el estimador y el informe hacen las mismas preguntas de siempre y reciben respuestas distintas: ninguno pregunta si una actividad es nocturna, y no hay `instanceof` ni `if` por variante. `ExpeditionReporter`, `ExpeditionEstimator`, `RiskToleranceValidation` y `MissingResourcesValidation` no se tocaron: la iluminación que falta la reporta la regla de recursos como cualquier equipo, y el riesgo mayor lo levanta la de tolerancia como advertencia.
  - `ActivityRulesDecorator` delega **todos** los métodos de `ActivityRules`, incluidos los que tienen respuesta por defecto. Si un decorador heredara el `default` de la interfaz, respondería vacío y perdería lo que aporta la regla envuelta (por ejemplo, las certificaciones que exige otro decorador). Con la base, un decorador nuevo solo redefine lo que cambia y no puede olvidarse de un método. Es el mismo esquema que `ScheduledResource` (D9) e `ItineraryBlock` (D39): la interfaz es el contrato y la clase abstracta, la implementación compartida.
  - El factor de riesgo es "un nivel más" porque `RiskLevel` es una escala ordinal, sin una magnitud que multiplicar. `RiskLevel.next()` devuelve el nivel siguiente y se queda en el máximo. Se agregó `EXTREME` por encima de `HIGH`: sin él, un buceo profundo (`HIGH`) tendría el mismo riesgo de día que de noche y la estimación no reflejaría el factor nocturno. Todo lo que compara riesgos lo hace por orden (`isAbove`, el máximo de un bloque), así que el nivel nuevo no obligó a tocar nada.
- **Alternativas descartadas:**
  - Una clase nocturna por tipo (`NightDivingRules`, `NightSampleCollectionRules`, ...) — duplica cada tipo: seis clases más hoy y una más por cada tipo futuro, con las mismas cuatro reglas nocturnas copiadas en cada una.
  - Un booleano `night` en `Activity` con condicionales — cada validación, el estimador y el informe tendrían que preguntar `if (activity.isNight())`. Es justo lo que la consigna pide evitar, y cada variante nueva sumaría otro booleano y otra ronda de condicionales.
  - Herencia (`NightDivingRules extends DivingRules`) — la misma explosión de clases, y además ata la variante a cada tipo concreto: no se le puede aplicar a un tipo que todavía no existe ni combinar con otra variante.
  - Agregar la certificación nocturna a cada `StaffRequirement` de la regla base — ver D43.
  - Que cada tipo implemente su variante (un método `atNight()` en `ActivityRules`) — obligaría a cada tipo a conocer las reglas nocturnas y a repetirlas.
- **Consecuencia:**
  - Otra variante transversal (por ejemplo, una operación en altura) es otra clase que extiende `ActivityRulesDecorator`, y se puede apilar sobre la nocturna.
  - Nada impide envolver dos veces la misma regla: el riesgo subiría dos niveles y la iluminación quedaría pedida dos veces. El registro de tipos envuelve una sola vez (D44); queda como deuda técnica de F1.
  - Una regla envuelta no dice que es nocturna. Es a propósito, para que nadie decida por eso; si hace falta mostrarlo (por ejemplo en la respuesta de la API), el dato tiene que viajar aparte.
  - `EXTREME` es un valor nuevo de un `enum` que la API va a serializar: un cliente que conocía tres niveles tiene que aceptar el cuarto.

### D43 — Permisos y certificaciones que exige la actividad (Entrega 2, F1, extiende D10 y D13)

- **Origen:** dos de los requisitos de F1 no entraban en el modelo. Las certificaciones se pedían por requisito de personal (`StaffRequirement`, D11) y la validación contaba calificados contra el cupo, así que una persona de más sin la certificación pasaba. Los permisos solo los exigía la zona (D13), a toda expedición que trabajara en ella.
- **Patrón / principio:** Strategy (D1) con respuesta por defecto + Information Expert (D13, D17) + identidad del hallazgo por regla y sujeto (D32).
- **Dónde:** `ActivityRules.requiredCertificationsForAllStaff()` y `requiredPermitsIn(Zone)`, con respuesta vacía por defecto; `Activity.requiredCertificationsForAllStaff()`, `admits(Certifiable)`, `requiredPermits()` y `missingPermits(grantedPermits)`; `Zone.specialPermitFor(operation)`; `MissingCertificationsValidation` y `MissingPermitsValidation`; el caso de uso `FindAssignmentCandidates`.
- **Por qué:**
  - Son dos preguntas nuevas a las reglas de una actividad, y las dos tienen una respuesta por defecto: nada. Los seis tipos y los dobles de prueba que implementan `ActivityRules` no cambiaron; solo responde algo distinto quien lo necesita, hoy `NightOperationRules` (D42).
  - **Certificación para todo el personal.** No es un requisito de cupo sino una condición para participar. La actividad dice qué certificaciones exige a cada persona asignada y `Activity.admits` responde si una persona puede participar, con el mismo criterio que `StaffRequirement.qualifies` (D17): decide quien conoce el requisito. `MissingCertificationsValidation` suma una crítica por cada persona asignada a la que la actividad no admite, con sujeto actividad + persona (D32). Así, una tercera persona sin la certificación en un buceo nocturno de dos buzos se reporta aunque el cupo esté cubierto, y el mensaje dice quién es.
  - **Permiso especial por zona.** La zona sigue siendo la dueña de sus permisos (D13): `Zone.specialPermitFor("night operations")` devuelve el permiso que esa zona exige para ese tipo de operación. La regla nocturna no arma el permiso: se lo pide a la zona de la actividad.
  - **Cómo se llama el permiso.** Es un `Permit` como cualquier otro, con el nombre `<operación> in zone <id de la zona>`; para la zona `z-1`, `night operations in zone z-1`. La operación se normaliza como en D34 (sin espacios alrededor y en minúsculas) y el id de la zona va tal cual. Lleva el id y no el nombre de la zona porque el id es único (`ZoneRepository` rechaza uno repetido) y dos zonas pueden llamarse igual (D15). Como la zona es parte del nombre, el permiso de una zona es otro permiso para cualquier otra y no la cubre. Quien concede el permiso lo obtiene de `zone.specialPermitFor(...)` o lo escribe con esa convención, y lo agrega a los permisos concedidos como cualquier otro (`ExpeditionBuilder.addPermit`, `CreateExpeditionCommand.grantedPermits`). No hubo que cambiar `Permit`, la expedición ni los comandos.
  - `requiredPermitsIn(Zone)` recibe la zona en vez de que la regla la guarde: D11 ya preveía agregar el parámetro si una regla necesitaba un dato común de la actividad. La zona sigue siendo un dato de `Activity`, que la pasa al preguntar (`Activity.requiredPermits()`), y unas mismas reglas sirven en cualquier zona.
  - `MissingPermitsValidation` reporta, además de los permisos de las zonas (con sujeto expedición, como antes), los que exige cada actividad pendiente y la expedición no tiene concedidos, como crítica con sujeto actividad. Mira solo las pendientes por el criterio de D33: lo que ya se hizo no se vuelve a validar. La comparación la hace `Activity.missingPermits`, en espejo con `Zone.missingPermits` (D13).
  - `FindAssignmentCandidates` ofrece para cada requisito de personal solo a quienes la actividad admite. Sin eso, el catálogo propondría para una actividad nocturna personas que la validación después rechaza.
  - No hay reglas nuevas: son las mismas dos, con una definición más amplia de "falta una certificación" y "falta un permiso" (mismo criterio que D41). `ApprovalPolicy` no se tocó y las reglas obligatorias siguen siendo siete (D31).
- **Alternativas descartadas:**
  - Agregar la certificación nocturna a cada `StaffRequirement` de la regla base, como proponía el plan — no cubre lo que pide la consigna ("todo el personal asignado"): la validación cuenta calificados contra el cupo y la persona de más pasa. Y sumada a la exigencia por persona, la misma falta saldría reportada dos veces, una por requisito y otra por persona.
  - Agregar el permiso nocturno a los `requiredPermits` de la zona — se le exigiría a toda expedición que trabaje en la zona, aunque no tenga ninguna actividad nocturna.
  - Un permiso genérico `Night operations`, sin zona — el permiso de una zona cubriría a todas las demás.
  - Agregar la zona como campo de `Permit` — cambiaría un value object que usan la zona, la expedición y los comandos, y la forma de conceder un permiso, para un caso que el nombre ya resuelve.
  - Que el decorador reciba la zona en el constructor — duplicaría un dato de `Activity` y permitiría reglas armadas para una zona dentro de una actividad de otra.
  - Una regla de validación nueva para los requisitos nocturnos — tendría que preguntar si la actividad es nocturna, o repetir lo que ya hacen las dos reglas existentes.
  - Rechazar al asignar a una persona sin la certificación — las asignaciones cambian mientras se planifica y un borrador puede estar incompleto a propósito (D21).
- **Consecuencia:**
  - El nombre del permiso especial es una convención de texto y los permisos se comparan por nombre exacto (D8): concederlo mal escrito deja la crítica en pie, como pasa con cualquier permiso. Si los permisos ganan reglas propias (vigencia, autoridad que los emite), el lugar para cambiarlo es `Permit`.
  - Si una regla y un requisito de personal piden la misma certificación, la falta se reporta por los dos caminos. Hoy no pasa: ningún tipo pide `Night operations` en un `StaffRequirement`.

### D44 — Registro de tipos de actividad y cuatro tipos nuevos (Entrega 2, F1, extiende D1)

- **Origen:** la demostración pide seis tipos de actividad y había dos. Además, la API y el cargador del seed reciben el tipo como texto: hace falta pasar del nombre de un tipo y sus parámetros a una `ActivityRules` (D38 lo había dejado pendiente).
- **Patrón / principio:** Registry + cada tipo como un objeto que sabe crear sus reglas + Open/Closed; el nombre del tipo es un value object que se normaliza (D34).
- **Dónde:** interfaz `ActivityType` (en `business.interfaces.activities`); `ActivityTypeName` y `ActivityParameters` (en `business.models.activities`); en `business.rules`: `ActivityTypeRegistry`, `SingleParameterActivityType` y las reglas nuevas `DroneSurveyRules`, `WildlifeObservationRules`, `WaterQualityMeasurementRules` y `TransferRules`; la constante `TYPE` de cada una de las seis reglas; `UnknownActivityTypeException` e `InvalidActivityParametersException` (en `business.exceptions`); el caso de uso `QueryActivityTypes`; los beans `ActivityTypeRegistry` y `QueryActivityTypes` en `FieldOpsConfig`.
- **Por qué:**
  - Los cuatro tipos nuevos siguen a los dos que había (D11): cada uno recibe su dato propio en el constructor, lo valida y calcula con constantes con nombre. Los seis tipos, con el nombre y el parámetro con que se piden al registro:

    | Tipo | Parámetro | Duración | Riesgo | Personal | Equipamiento | Consumibles |
    |---|---|---|---|---|---|---|
    | `sample collection` | `sampleCount` | 1 h + 20 min por muestra | bajo | 1 persona | 1 `cooler` | 1 `sample container` por muestra |
    | `diving` | `maxDepthMeters` | 45 min + 2 min por metro | medio; alto a más de 30 m | 2 con `diving` | 1 `boat` y 2 `oxygen tank` | 20 litros de `fuel` |
    | `drone survey` | `areaHectares` | 30 min + 3 min por hectárea | bajo; medio a más de 50 ha | 1 con `drone pilot` | 1 `drone` | ninguno |
    | `wildlife observation` | `observationPoints` | 30 min + 45 min por punto | bajo | 2 personas | 2 `binoculars` | ninguno |
    | `water quality measurement` | `stationCount` | 30 min + 15 min por estación | bajo | 1 con `water analysis` | 1 `multiparameter probe` | 1 `reagent kit` por estación |
    | `transfer` | `distanceKilometers` | 30 min + 2 min por kilómetro | bajo; medio a más de 100 km | 1 con `off-road driving` | 1 `truck` | 0,15 litros de `fuel` por kilómetro |

  - Cada tipo es un objeto `ActivityType`: dice su nombre, los nombres de sus parámetros y cómo crear sus reglas (`rulesWith`). `ActivityTypeRegistry` guarda los tipos por nombre y le pide las reglas al que corresponde. No hay `switch` ni `if` por tipo: el código que busca un tipo y crea sus reglas no cambia cuando se suma uno.
  - La descripción de cada tipo vive en su regla, como constante `TYPE` (`DivingRules.TYPE`). El nombre del tipo y el de su parámetro son datos propios del tipo (D11) y quedan al lado del constructor que describen.
  - Los seis tipos toman un entero, así que comparten una implementación, `SingleParameterActivityType` (nombre, parámetro y una referencia al constructor), en vez de seis clases iguales. Un tipo con otra forma (dos parámetros, o ninguno) implementa `ActivityType` directamente.
  - El nombre del tipo es un value object, `ActivityTypeName`, que se normaliza al construirse igual que las categorías y las certificaciones (D34): `Diving`, `diving` y ` DIVING ` son el mismo tipo. Los nombres de los parámetros no se normalizan: son identificadores (`maxDepthMeters`) y se comparan tal cual, como los publica la consulta de tipos.
  - Pedir mal un tipo falla con una excepción de dominio. `UnknownActivityTypeException` nombra el tipo pedido y lista los disponibles. `InvalidActivityParametersException` dice qué tipo no se pudo crear y por qué: falta el parámetro, o su valor no sirve. La validación del valor sigue estando en la regla (D16): el tipo traduce su `IllegalArgumentException` y le agrega el contexto.
  - La variante nocturna se pide con un indicador: `rulesFor(tipo, parámetros, atNight)` envuelve la regla base en `NightOperationRules` (D42). Es el único lugar que arma la variante a partir de un pedido, y la envuelve una sola vez.
  - El dominio define cuáles son los tipos disponibles (`ActivityTypeRegistry.withStandardTypes()`), con el mismo criterio que las reglas obligatorias de aprobación (D31). El constructor recibe la lista, así que los tests arman registros chicos; dos tipos con el mismo nombre se rechazan.
  - `QueryActivityTypes` lista los tipos disponibles con sus parámetros, para que la API pueda publicarlos (D38).
- **Alternativas descartadas:**
  - Un `switch` por nombre en el registro, o un `enum` de tipos con un `switch` — cada tipo nuevo obligaría a modificar el código que crea las reglas. Es el antipatrón que D1 ya había descartado.
  - Una clase `ActivityType` por tipo (`DivingType`, `TransferType`, ...) — seis clases que solo cambian en un nombre y un constructor.
  - Descubrir los tipos por reflexión o recorriendo el classpath — esconde qué tipos existen y falla recién en ejecución.
  - Armar el registro en `web` con beans de Spring — el cargador del seed y los tests del dominio lo necesitan sin Spring, y el dominio no puede depender de `web` (D36, D37).
  - Registrar la variante nocturna como tipos aparte (`night diving`, ...) — duplicaría cada entrada y volvería a atar la variante a cada tipo (D42).
  - Parámetros como texto libre (`Map<String, String>`) — habría que interpretarlos dentro del dominio, y hoy todos son enteros.
- **Consecuencia:**
  - Sumar un tipo es escribir su regla con su `TYPE` y agregar una línea a `withStandardTypes()`.
  - Los parámetros son enteros. Si un tipo necesita otro dato (un decimal, un texto), hay que ampliar `ActivityParameters`.
  - ~~`ScheduleActivityCommand` sigue recibiendo la `Activity` ya armada (D38). Que reciba el tipo, los parámetros y el indicador de nocturna queda pendiente para la API, porque además hay que resolver por id la zona y las dependencias.~~ **Resuelto en D53:** el comando recibe el nombre del tipo, sus parámetros y el indicador de nocturna, y `ScheduleActivity` es el único cliente de `rulesFor` fuera de los tests.
- **Revisado en D53:** `rulesFor(tipo, parámetros, atNight)` pasa a usarse de verdad, desde el caso de uso que agenda una actividad, y `QueryActivityTypes` queda publicado en `GET /api/activity-types`, que es el endpoint que le dice al cliente qué tipos y qué parámetros puede pedir.

### D45 — El incidente se publica como evento de dominio, con entrega síncrona (Entrega 2, F3)

- **Origen:** F3 pide que, al registrarse un incidente, el sistema proponga solo un plan alternativo. Registrar el incidente y armar la propuesta son dos responsabilidades distintas: la primera es del seguimiento (D23) y la segunda, de la replanificación (D25, D33).
- **Patrón / principio:** evento de dominio + Observer (publicador y suscriptores) detrás de un puerto de salida (D35). La entidad no publica: publica el caso de uso, después de que la entidad registró el incidente.
- **Dónde:** `DomainEvent` y `EventSubscriber<E>` (en `business.interfaces.events`); `IncidentRegistered(expedición, incidente)` (en `business.models.events`); puerto `EventPublisher` (en `business.providers`); caso de uso `RegisterIncident` con `RegisterIncidentCommand`; suscriptor `ProposeReplan` (en `business.usecases`); adaptador `InMemoryEventPublisher` (en `app`); `ReplanningConfig` (en `web`) arma el publicador con la lista de suscriptores que encuentra Spring.
- **Por qué:**
  - `RegisterIncident` busca la expedición y la actividad, le pide a la expedición que registre el incidente (`Expedition.registerIncident`, que valida el estado y la actividad) y publica `IncidentRegistered`. No sabe que alguien arma una propuesta: si mañana un incidente además tiene que avisar a alguien o actualizar un tablero, se suma un suscriptor sin tocar el registro (Open/Closed).
  - La entrega es síncrona: `publish` vuelve cuando todos los suscriptores terminaron. Así, "al volver de `RegisterIncident` ya existe la propuesta" es una garantía y no una carrera, y el caso de uso devuelve la propuesta generada (la busca por incidente en el repositorio de propuestas). Si no hay ninguna, falla con `IllegalStateException`: es un error de armado (falta el suscriptor), no de negocio.
  - Cada suscriptor declara el tipo de evento que atiende (`eventType()`). El publicador en memoria se lo entrega a los que corresponden, en el orden en que los recibió. La comparación de tipos queda en el adaptador, no en el dominio.
  - El evento lleva el id de la expedición y el incidente completo (un `record` inmutable), así el suscriptor no depende de volver a leer el incidente.
  - La entidad no publica eventos ni guarda una lista de pendientes: con el repositorio en memoria las entidades se modifican en el lugar (D38) y no hay un "al guardar" donde despacharlos. Publicar desde el caso de uso deja el orden explícito: primero registra, después avisa.
- **Alternativas descartadas:**
  - Que `RegisterIncident` llame directo al replanificador — acopla el registro con la replanificación; cada reacción nueva a un incidente obligaría a modificarlo.
  - Usar el `ApplicationEventPublisher` de Spring dentro del dominio — rompe D36 y D37: `domain` no puede depender de Spring, y el enforcer lo prohíbe.
  - Entrega asíncrona (otro hilo o una cola) — la propuesta podría no existir cuando vuelve la llamada, que es justo lo que pide el criterio de aceptación; y sumaría reintentos y orden de entrega sin necesidad.
  - Un puerto por evento (`IncidentListener`) — cada evento nuevo sería un puerto nuevo con su adaptador.
  - Que la entidad acumule los eventos y el repositorio los despache al guardar — hoy no existe ese "guardar" (D38).
- **Consecuencia:** no hay transacción ni reintentos. Si el suscriptor falla, la excepción llega a quien registró el incidente, pero el incidente ya quedó registrado en la expedición. Con persistencia real, registrar y proponer tienen que ir en la misma transacción o pasar a un outbox. Queda como deuda técnica de F3.

### D46 — La propuesta ofrece varias opciones, una por generador, y solo las que se pueden armar (Entrega 2, F3, revisa D25, D33 y D40)

- **Origen:** F3 pide proponer "reprogramación, reemplazo de recursos o cancelación parcial". D33 ya tenía un motivo por cada caso (`Delay`, `ResourceWithdrawn`, `Cancellation`), pero descartó elegir solo el reemplazo de un recurso y dejaba fallar las alternativas que no se podían armar (un retraso que pisa al hermano siguiente de un secuencial, D40; una cancelación con dependientes, D25).
- **Patrón / principio:** Strategy para cada tipo de opción (`ReplanOptionGenerator`), en una lista inyectada (Open/Closed) + reutilización del replanificador para decidir qué se puede armar (DRY) + Information Expert (el itinerario sabe qué se mueve o cae con una actividad).
- **Dónde:** interfaz `ReplanOptionGenerator` (en `business.interfaces.replanning`); `RescheduleOptions`, `ResourceReplacementOptions` y `CancellationOptions` (en `business.replanning`); `ReplanOption(motivo, resumen)`; el motivo nuevo `ResourceReplaced` y `ResourceAssignment`; `ExpeditionReplanner.canPlan`; `Itinerary.dependentsOf` y `movingWith`; `ItineraryNode.itemsAfter`; `ReplannedActivity.additions`; el suscriptor `ProposeReplan`.
- **Por qué:**
  - Cada generador mira el incidente y propone opciones de un solo tipo, cada una con su `ReplanReason` y un resumen legible. `ProposeReplan` junta las de todos, en el orden de la lista (reprogramar, reemplazar, cancelar), y se queda con las que el replanificador puede armar. Sumar un tipo de opción es sumar una clase y una línea en `ReplanningConfig`.
  - **Qué se puede armar lo decide el replanificador, no el generador.** `canPlan` arma la alternativa de prueba con un id descartable y responde si lo logró (`InvalidScheduleException` es "no se puede"). No guarda nada ni toca recursos, y no mira el estado de la original, porque la propuesta se hace con la expedición todavía en ejecución. Así, las reglas de qué es un plan válido (D21, D40) siguen en un solo lugar y una opción ofrecida siempre se puede aceptar si nada cambió en el medio.
  - **Reprogramación.** El retraso lo declara quien informa el incidente (`IncidentReport.delayingBy`): "el motor se arregla en dos horas" lo sabe quien está en el campo. Sin retraso declarado no se ofrece. Elegir un retraso por nuestra cuenta (una escalera fija o un valor configurado) sería un número arbitrario.
  - **El retraso arrastra lo que viene después (resuelve la deuda de D40).** Cuando una actividad vuelve más tarde, se corren por el mismo tiempo las actividades que vienen después de ella en cualquier bloque secuencial que la contenga (`ItineraryNode.itemsAfter`, que el secuencial responde con sus hermanos siguientes y que se compone hacia afuera) y las que dependen de ella, y a su vez lo que viene después de esas y lo que depende de ellas (`Itinerary.movingWith`, un cierre transitivo). Una rama paralela hermana no se corre: puede seguir en su horario. Si algo que habría que correr ya empezó, o se sale de su ventana o del período de la expedición, la alternativa no se puede armar y la opción no se ofrece.
  - **Reemplazo de recurso.** Solo si el incidente nombra un recurso asignado a la actividad (`IncidentReport.affecting`). Los candidatos salen del catálogo con las mismas consultas de siempre (D18): los que cubren el requisito que cubría el recurso retirado, están libres en el horario de la actividad y, si es personal, la actividad los admite (D43); se dejan afuera los que ya están asignados a esa actividad. Se ofrece una opción por candidato, en el orden del catálogo y hasta tres (`MAX_CANDIDATES`), para que el responsable elija quién y no lo decida un criterio que el enunciado no da (D33). Sin candidatos no hay opción de reemplazo.
  - `ResourceReplaced(retirado, reemplazo)` es el motivo nuevo: deja la actividad en su horario, sin el recurso retirado y con el reemplazo. Para que el replanificador pueda asignarlo sin preguntar de qué tipo es, el reemplazo viaja como `ResourceAssignment`, que se crea con `of(Person)`, `of(Equipment)` u `of(DepletableResource)` y guarda la forma de asignarlo. `ReplannedActivity` sumó la lista `additions`, vacía para los demás motivos.
  - **Cancelación parcial (en cascada).** Siempre se ofrece. Cancela la actividad afectada y todas las que dependen de ella, directa o indirectamente (`Itinerary.dependentsOf`); el resumen las nombra. Si alguna ya empezó, no se puede armar.
  - La cascada y el arrastre son del replanificador, no de cada motivo: `ReplanReason` sigue respondiendo una sola pregunta sobre la actividad afectada (D33), y el replanificador saca las consecuencias de esa respuesta para el resto del itinerario (vuelve más tarde: se corre lo que viene después; no vuelve: cae lo que depende). Por eso vale igual para la replanificación manual (`ReplanExpedition`) que para las propuestas.
  - Una propuesta sin opciones se guarda igual: queda registrado que el sistema no encontró alternativa, y el responsable solo puede rechazarla (y replanificar a mano si quiere).
- **Alternativas descartadas:**
  - Un único método que arme todas las opciones con un `switch` por tipo — cada tipo nuevo obligaría a modificarlo.
  - Que cada generador verifique por su cuenta si su opción se puede armar — repetiría las reglas de agenda y de bloques en tres lugares.
  - Crear las alternativas al generar la propuesta y guardarlas en borrador — llenaría el repositorio de expediciones que nadie eligió.
  - Elegir el primer candidato del catálogo, o una opción con una lista de candidatos para elegir al aceptar — la primera decide por el responsable; la segunda obliga a que aceptar reciba además un recurso.
  - Una "retirada sin reemplazo" (`ResourceWithdrawn`) como opción cuando no hay candidatos — la alternativa nacería con un faltante crítico; el motivo sigue existiendo para la replanificación manual.
  - Mantener la deuda de D40 y no ofrecer el retraso dentro de un secuencial — en la demo el caso típico es justamente una actividad en medio de una secuencia.
  - Que la cascada sea propia de cada motivo (`Delay` corriendo hermanos) — cada motivo tendría que conocer el itinerario.
- **Consecuencia:**
  - Agregar un tipo de opción es una clase que implementa `ReplanOptionGenerator`.
  - `canPlan` arma la alternativa entera por cada opción. Con el tamaño de un itinerario no pesa.
  - Cambia el comportamiento de la replanificación manual: una cancelación con dependientes ya no falla (D25) y un retraso dentro de un secuencial ya no falla (D40). Los tests que lo esperaban se actualizaron.
  - Que un candidato esté libre se verifica contra el calendario completo, incluidas las reservas de la propia original. No es un problema: un recurso reservado por la original en ese mismo horario está asignado a esa actividad o a una paralela.

### D47 — Ciclo de vida de la propuesta y qué pasa con la expedición al decidir (Entrega 2, F3, revisa D33)

- **Origen:** criterio de aceptación de F3: el responsable puede aceptar o rechazar. D33 solo replanificaba una expedición aprobada o suspendida y con la actividad afectada sin empezar; el incidente llega con la expedición en ejecución y puede afectar a una actividad en curso.
- **Patrón / principio:** caso de uso para una operación que coordina varios objetos (D28) + todo o nada (D27) + estados de la propuesta como `enum` con un único estado de origen.
- **Dónde:** `ReplanProposal` y `ReplanProposalStatus` (pendiente, aceptada, rechazada, reemplazada por otra) en `business.models.replanning`; `ReplanDecision`; caso de uso `DecideReplanProposal` (`accept`, `reject`) con `ReplanDecisionCommand`; `Expedition.suspendToReplan()` y `requireResponsible()`; `ProposalAlreadyDecidedException` y `NotAResponsibleException`.
- **Por qué:**
  - **Quién y cómo decide.** Decide un responsable de la expedición (`requireResponsible`), con una justificación no vacía, tanto para aceptar como para rechazar: las dos son decisiones que quedan auditadas. La fecha la pasa quien llama, igual que la de incidentes y observaciones (D23). Una propuesta se decide una sola vez.
  - **Aceptar** recibe el número de opción (de 1 en adelante). Primero verifica todo: que la propuesta siga pendiente, que la opción exista, que quien decide sea responsable, que la opción todavía se pueda armar (`canPlan`, por si el itinerario cambió desde la propuesta) y que no haya actividades en curso. Recién entonces suspende la expedición si está en ejecución (`suspendToReplan`), arma la alternativa en borrador con `ReplanExpedition` (D33) y registra la decisión con el id de la alternativa. Si algo falla antes, no cambió nada.
  - **Aceptar no reemplaza el plan en el acto.** La alternativa pasa por revisión y aprobación como cualquier expedición, y recién al aprobarla `ApproveExpedition` pasa la original a reemplazada y le transfiere las reservas (D33). Mientras tanto la original queda suspendida: el campo deja de seguir un plan que va a cambiar. Si la alternativa nunca se aprueba, se reanuda la original (`resume`), como cualquier suspensión.
  - **Rechazar** no toca la expedición: sigue en ejecución con su plan.
  - **Actividad afectada en curso.** El incidente se registra igual y la propuesta se arma. Para aceptar, la actividad tiene que estar cerrada, con el resultado real que conoce quien la ejecutó (D29): aceptar con una actividad en curso falla con `ActivitiesInProgressException`. En la alternativa, la actividad afectada se rehace según la opción (reprogramada o con el reemplazo) aunque en la original haya terminado; si la opción es cancelarla, queda cerrada con lo que llegó a hacerse. La original conserva el intento.
  - **Varias propuestas pendientes.** Cada incidente tiene la suya. Al aceptar una, las demás pendientes de la misma expedición pasan a reemplazadas por la aceptada (`SUPERSEDED`, con el id de la que se aceptó): ya no se pueden decidir, porque se pensaron sobre un plan que dejó de estar vigente. Quedan auditadas.
  - `suspendToReplan` no agrega transiciones: usa las de la tabla (D20). Suspende solo si la expedición puede pasar a suspendida y después exige que pueda pasar a reemplazada, así un borrador o una expedición terminada fallan sin cambiar.
- **Alternativas descartadas:**
  - Suspender al registrar el incidente — un incidente menor pararía la expedición aunque la propuesta se rechace.
  - Agregar en ejecución → reemplazada a la tabla — la original seguiría ejecutándose mientras la alternativa está en revisión, y las actividades que se cierren en ese lapso no estarían en la alternativa.
  - Cerrar sola la actividad en curso al aceptar — habría que inventar la hora de fin y el resultado, que D29 dejó en manos de quien la ejecuta.
  - Dejar pendientes las demás propuestas — aceptar otra crearía una segunda alternativa de la misma original, y la que se aprobara segunda fallaría.
  - Que la fecha de la decisión la ponga un `Clock` inyectado — sería el único lugar del dominio que toma la hora del sistema.
- **Consecuencia:** entre aceptar y aprobar la alternativa, la expedición no registra seguimiento (está suspendida) ni nuevos incidentes (D23). Si hace falta seguir trabajando, se reanuda la original y la alternativa queda sin aprobar.

### D48 — Auditoría: la propuesta es el registro, el plan original sigue siendo la expedición original (Entrega 2, F3, revisa D23)

- **Origen:** criterio de aceptación de F3: el plan original, el incidente y la decisión quedan auditados y consultables.
- **Patrón / principio:** la propuesta como registro inmutable de lo que pasó, una vez decidida + reutilización de lo que ya conserva el historial (D25, D33) en vez de un mecanismo nuevo.
- **Dónde:** `ReplanProposal` (expedición original, incidente, opciones, estado, decisión, opción elegida, alternativa, la que la reemplazó); puerto `ReplanProposalRepository` (`nextId`, `add`, `findById`, `findByIncident`, `findByExpedition`) con `InMemoryReplanProposalRepository` en `app` y su contrato `ReplanProposalRepositoryContract`; caso de uso `QueryReplanProposals` (por id y por expedición); `Incident`, `IncidentId` e `IncidentReport`; `Expedition.registerIncident` y `getIncidents`; `ExpeditionBuilder.addInheritedIncident`; `ExpeditionReporter`.
- **Por qué:**
  - **El plan original** es la expedición original: no se modifica más allá de su estado (D25) y queda consultable por id con `QueryExpeditions`. Las dos expediciones están vinculadas en los dos sentidos (`getReplacedExpedition`, `getReplacement`, D33) y la propuesta guarda el id de las dos.
  - **El incidente** queda en la bitácora de la expedición y, copiado, en la propuesta. Antes vivía en `ActivityTracking`, que solo existe cuando la actividad empezó; ahora un incidente puede afectar a una actividad pendiente, así que la expedición lleva una sola lista con todos (`getIncidents`), cada uno con su id, la actividad que afecta, el recurso y el retraso que se informaron. El informe los toma de ahí (D24).
  - **El id del incidente lo pone la expedición:** `IncidentId(expedición, número)`, del 1 en adelante en el orden de registro. Es único porque el id de la expedición lo es (D26), y no hace falta un puerto nuevo.
  - **La alternativa hereda la bitácora** de la que reemplaza, con los ids originales: su informe sigue mostrando la salida entera (D33), incluido el incidente que la motivó.
  - **La decisión** queda en la propuesta: quién, por qué, cuándo, qué opción y qué alternativa generó; o quién la rechazó y por qué; o qué propuesta la dejó sin efecto.
  - El id de la propuesta lo genera el repositorio, como el de la expedición (D26).
- **Alternativas descartadas:**
  - Event Sourcing (reconstruir la expedición a partir de sus eventos) — ver la tabla de patrones no aplicados.
  - Guardar el incidente solo en la propuesta — el informe de la expedición dejaría de contar los incidentes.
  - Mantener los incidentes en `ActivityTracking` y sumar otra lista para los de actividades pendientes — el mismo concepto en dos lugares.
  - Copiar el itinerario original dentro de la propuesta — duplicaría la expedición original, que ya está guardada y no cambia.
  - Un repositorio de incidentes con su propio generador de ids — el incidente es parte de la expedición y no se consulta suelto.
- **Consecuencia:** el informe de una alternativa cuenta también los incidentes de la original. Una propuesta no se borra ni se edita después de decidida.

### D49 — La API REST es un adaptador driving, con DTOs propios y el mapeo de errores fuera del dominio (Entrega 2, F5)

- **Origen:** la Entrega 2 pide exponer los casos de uso por una API REST. El hexágono ya estaba cerrado (D35) y los casos de uso ya reciben comandos por id (D38), así que faltaba el adaptador de entrada: qué sale por HTTP, con qué forma, y qué código devuelve cada error del dominio.
- **Patrón / principio:** adaptador driving (un puerto de entrada visto desde afuera, D35) + DTO como única frontera de serialización + traducción de errores en el borde: el dominio no conoce HTTP.
- **Dónde:** módulo `web`, paquete `adapters.driving.rest`:

  | Pieza | Qué hace |
  |---|---|
  | `ExpeditionController` | El recurso expediciones: `POST /api/expeditions`, `GET /api/expeditions` y `GET /api/expeditions/{id}`, sobre `CreateExpedition` y `QueryExpeditions`. Un controller por recurso |
  | `rest.dtos` | `CreateExpeditionRequest`, `ExpeditionResponse`, `PeriodDto` y `RestrictionsDto`, todos `record` |
  | `rest.mappers.ExpeditionMapper` | De la petición a `CreateExpeditionCommand` y de `Expedition` a `ExpeditionResponse`. Es el único lugar del adaptador que habla los dos vocabularios |
  | `DomainExceptionHandler` | `@RestControllerAdvice` con la tabla de las dieciocho excepciones de `business.exceptions` a código HTTP, y `ProblemDetail` (RFC 9457) como único formato de error |
  | `OpenApiConfig` | Los datos del documento; springdoc publica `/v3/api-docs` y Swagger UI en `/swagger-ui.html` a partir de los controllers |

- **Por qué:**
  - **Ninguna entidad de dominio se serializa y ningún DTO entra al dominio.** Si se devolviera `Expedition`, su forma interna pasaría a ser el contrato público: cualquier campo nuevo o renombrado rompería a los clientes, el grafo se arrastraría entero (zonas, personas, itinerario, incidentes) y habría que anotar el dominio para Jackson, que es exactamente lo que D36 prohíbe. Con DTOs propios, el contrato se cambia sin tocar el hexágono y al revés.
  - **La única excepción son los enums** (`ExpeditionStatus`, `RiskLevel`). Un enum es un nombre cerrado, no una entidad con comportamiento: volver a declararlo en `web` dejaría dos listas para mantener sincronizadas y el mismo valor en el documento de OpenAPI. Los value objects sí se traducen (`ExpeditionId` y `ResourceId` viajan como `String`, `TimePeriod` como `PeriodDto`), porque ahí lo que importa es la validación que hacen al construirse.
  - **La respuesta de la expedición no trae el itinerario.** El árbol tiene su propio endpoint (`GET /api/expeditions/{id}/itinerary`) y cómo se serializa sin condicionales por tipo de nodo todavía no está decidido (`ItineraryNode` no expone los hijos ni el tipo). Anticiparlo acá habría fijado la decisión desde el recurso equivocado. Las listas de ids salen ordenadas, así que la misma expedición se lee siempre igual aunque el dominio las guarde en un `Set`.
  - **El mapeo de errores vive en el adaptador y no en las excepciones.** El dominio dice qué regla se rompió; que eso sea un 409 o un 422 es una decisión del protocolo. Es una tabla de un `Map` en `DomainExceptionHandler`: se lee de un vistazo y un test parametrizado la recorre entera. La búsqueda sube por la jerarquía, así que la entrada de `IllegalArgumentException` también cubre las que lanzan el JDK y Spring al convertir un valor. El handler extiende `ResponseEntityExceptionHandler` para que las excepciones de Spring MVC (un cuerpo que no se puede parsear, un método no soportado) conserven su respuesta estándar en vez de caer en el 500 genérico.
  - **`NotAResponsibleException` es 422 y no 403.** Hoy la API no tiene autenticación: quién decide llega como un id en el cuerpo del pedido, igual que la justificación y la fecha (D47). Un 403 afirmaría que hay un sujeto autenticado al que se le niega el acceso, y no lo hay; lo que falla es una regla de negocio sobre los datos enviados, como en `UnacceptedWarningException`. Cuando haya autenticación y el responsable salga de la credencial y no del cuerpo, pasa a ser 403.
  - **Las operaciones que no son CRUD son sub-recursos en `POST`, no verbos en la URL.** Aprobar es `POST /api/expeditions/{id}/approval`, suspender es `POST /api/expeditions/{id}/suspension`, cerrar es `POST /api/expeditions/{id}/completion`. La URL nombra la cosa que se crea (una aprobación, una suspensión) y el método dice qué se hace con ella, que es lo que esperan los clientes y las herramientas de HTTP. Con verbos (`/approve`, `/suspend`) la URL deja de identificar recursos y cada operación nueva inventa su propio nombre.
  - **Los DTOs no validan.** Un campo que falta viaja como `null` o vacío, el dominio lo rechaza con su propio mensaje y el handler lo convierte en 400. Así hay una sola fuente de verdad de las reglas: la que ya tienen `DomainArguments` y los constructores (D8).
  - **Deuda conocida de `NullPointerException` -> 400.** La fila es correcta mientras el dominio valide los argumentos obligatorios con `Objects.requireNonNull` (D16): un cuerpo sin período llega al constructor y el NPE que levanta es, efectivamente, un error del cliente. El costo es que un NPE que sea un bug del adaptador (un mapper que desreferencia algo que no miró) se reporta igual: como 400, con su mensaje interno en el `detail`, en vez de como el 500 sin detalle que le corresponde. Se resuelve cuando el dominio use una excepción propia de argumento inválido en vez de `requireNonNull`, o cuando los mappers garanticen que nunca pasan `null` sin querer; la señal para hacerlo es el primer 400 que no se pueda explicar con el cuerpo del pedido.
  - **springdoc 3.1.1.** Es la línea construida sobre Spring Boot 4 (su pom padre es `spring-boot-starter-parent` 4.1.0, y el proyecto usa 4.1.1); la 2.x apunta a Boot 3 y no sirve acá. Que el documento se genere de verdad lo verifica un test, porque un build que compila no lo prueba.
- **Alternativas descartadas:**
  - Serializar las entidades de dominio y ahorrarse los DTOs y los mappers — ataría el contrato público a la forma interna del hexágono y obligaría a anotar `domain` para Jackson, contra D36.
  - Anotar las excepciones de dominio con su código HTTP (`@ResponseStatus` o un campo propio) — metería una decisión de protocolo en `business.exceptions`, que es la capa que justamente no tiene que saber que hay HTTP. Con `@ResponseStatus` además haría falta Spring en `domain`, que el enforcer prohíbe.
  - Verbos en la URL para lo que no es CRUD — ver arriba.
  - Bean Validation (`@Valid` y anotaciones en los DTOs) — ver la tabla de patrones no aplicados.
  - Bajar Spring Boot a 3.x para poder usar springdoc 2.x — cambiar el framework de toda la aplicación para acomodar una herramienta de documentación es al revés; la línea 3.x de springdoc existe y es la compatible.
- **Consecuencia:** cada recurso nuevo son dos piezas (su controller y sus DTOs con su mapper) y una fila en la tabla de errores si trae una excepción nueva; un test verifica que no quede ninguna excepción de `business.exceptions` sin clasificar. Esta entrada se completa en los PRs siguientes con por qué no se expone la replanificación manual (`ReplanExpedition`).
- **Completado en D54:** cómo se serializa el árbol del itinerario sin condicionales por tipo de nodo, con el tercer tramo del PR 5b.

### D52 — Casos de uso de alta y consulta del catálogo y de las zonas (Entrega 2, F5, extiende D38)

- **Origen:** hasta este PR no había forma de cargar nada. `ResourceCatalog` tenía `addStaff`, `addEquipment` y `addSupply`, y `ZoneRepository` tenía `add`, pero ningún caso de uso los usaba: una expedición solo se podía crear desde un test que armara en Java la zona y la persona que referencia. El adaptador REST recibe ids y texto, y no puede ser el que construya `Person`, `Vehicle`, `Instrument`, `Depletable` ni `Zone`.
- **Patrón / principio:** caso de uso con comando por id como puerto de entrada (D38), un comando `record` por alta, y agrupación por cohesión: una clase por conjunto de operaciones que hacen lo mismo, no una clase por operación.
- **Dónde:** en `business.usecases`, cinco casos de uso y cinco comandos; en `web`, un controller por recurso con sus DTOs y su mapper (D49):

  | Caso de uso | Métodos | Endpoints |
  |---|---|---|
  | `RegisterResources` | `registerPerson`, `registerVehicle`, `registerInstrument`, `registerSupply` | `POST /api/staff`, `/api/vehicles`, `/api/instruments`, `/api/supplies` |
  | `QueryCatalog` | `staff`, `person`, `vehicles`, `vehicle`, `instruments`, `instrument`, `supplies`, `supply` | `GET` de cada recurso y de `/{id}`, más `GET /api/equipment` |
  | `MarkResourceUnavailable` | `markStaffUnavailable`, `markVehicleUnavailable`, `markInstrumentUnavailable` | `POST /api/{staff\|vehicles\|instruments}/{id}/unavailabilities` |
  | `RegisterZone` | `register` | `POST /api/zones` |
  | `QueryZones` | `list`, `get` | `GET /api/zones`, `GET /api/zones/{id}` |

  Comandos: `RegisterPersonCommand`, `RegisterVehicleCommand`, `RegisterInstrumentCommand`, `RegisterSupplyCommand` y `RegisterZoneCommand`. La resolución de ids por tipo vive en `Lookups` (`vehicle`, `instrument`, `depletable` y `zone`, que además reemplaza la búsqueda que `CreateExpedition` tenía escrita a mano).
- **Por qué:**
  - **Cinco casos de uso y no uno por operación.** El plan anticipaba seis a título indicativo (`RegisterStaff`, `RegisterEquipment`, `RegisterSupply`, `QueryCatalog`, `MarkResourceUnavailable`, `RegisterZone`, `QueryZones`). Las cuatro altas quedaron juntas en `RegisterResources` por el mismo criterio con el que `TransitionExpedition` juntó las cinco transiciones: todas hacen lo mismo (armar el recurso desde su comando y dárselo al catálogo) y separarlas serían cuatro clases de tres líneas. La lectura va aparte (`QueryCatalog`) porque es el corte que ya existe entre `CreateExpedition` y `QueryExpeditions`, y las zonas repiten ese mismo corte. `MarkResourceUnavailable` no entró en `RegisterResources` porque no crea nada: busca un recurso que ya está y le delega al dominio, que es la forma de `AssignResources` y no la de un alta (y falla distinto: 404 en vez de 409).
  - **`Vehicle` e `Instrument` son dos sub-recursos, con un listado combinado en `/api/equipment`.** Los dos son `Equipment` para el dominio, pero no tienen la misma forma: el vehículo tiene patente y el instrumento no. Con dos altas separadas, cada cuerpo pide exactamente los campos de su tipo, el documento de OpenAPI dice la verdad sobre cuáles son obligatorios y no hace falta un discriminador que el mapper tenga que mirar para decidir qué construir. Quien busca equipo sin importar el tipo tiene `GET /api/equipment`, que responde un documento con dos listas (`vehicles` e `instruments`) en vez de un arreglo plano: así cada ítem conserva su forma y no aparece una patente que a veces es `null`, que es justamente lo que el sub-recurso evita.
  - **La indisponibilidad solo existe en los recursos reutilizables.** `markUnavailable` está en `ReusableResource`, no en `DepletableResource`: un consumible tiene stock, no calendario. Por eso `/api/supplies/{id}/unavailabilities` no existe (y el 404 de esa ruta lo cubre un test), y por eso el caso de uso tiene un método por tipo en vez de uno que resuelva "cualquier reutilizable": la URL dice `/api/vehicles/{id}`, así que un id que es de otro tipo no es otro recurso que se marca sin querer, es un pedido que no refiere a nada.
  - **El catálogo ya rechaza un id repetido**, con `DuplicateResourceException` y sobre un único espacio de ids (`registeredIds` es uno para personal, equipo y consumibles), así que el caso de uso no lo vuelve a chequear: un vehículo no puede tomar el id de una persona. Con las zonas pasa lo mismo del otro lado del puerto: `DuplicateZoneException` es parte del contrato de todo `ZoneRepository` (`ZoneRepositoryContract`), no una verificación del caso de uso. Las dos salen como 409 con la tabla de errores que ya estaba (D49): este PR no agregó excepciones.
  - **Las consultas devuelven las clases del modelo** (`Person`, `Vehicle`, `Instrument`, `Depletable`) y no las interfaces. Lo que la API muestra de un recurso (categoría, patente, stock) no es lo que las interfaces le preguntan: `Categorized` pregunta `belongsTo` y `DepletableResource` pregunta `hasStockFor`, que es la forma deliberada de D10 y D34 (comparar en un solo lugar en vez de repartir getters). Agregar esos getters a las interfaces debilitaría eso para todos los clientes del dominio; separar vehículos de instrumentos filtrando por tipo en la consulta no, porque ahí el tipo no decide comportamiento: selecciona. El modelo sí sumó accesores de lectura de lo que ya tenía guardado (`Vehicle`, `Instrument` y `Depletable` exponen su categoría, los tres certificables sus certificaciones y `Zone` sus permisos requeridos); las preguntas siguen estando y nadie está obligado a comparar valores crudos.
  - **El alta devuelve lo que quedó registrado, normalizado.** Las categorías y las certificaciones se normalizan al construirse (D34), así que una persona cargada con `"Diving"` se lee como `diving` y un bote cargado con `"Boat"` como `boat`. Es la única forma honesta de responder: es el valor con el que después se va a comparar.
  - **La indisponibilidad responde 204 y no 201.** No hay nada que leer después: `AvailabilityCalendar` no expone sus indisponibilidades, así que no habría `Location` que devolver ni `GET` que apuntar, y el cuerpo sería el mismo que mandó el cliente. El efecto observable es que el recurso deja de estar disponible, y eso lo verifica el test unitario del caso de uso. Cuando el calendario exponga lo que guarda, pasa a ser un 201 con su `Location`.
- **Alternativas descartadas:**
  - Que el controller llame directo a `ResourceCatalog` y `ZoneRepository` y construya los objetos de dominio — es lo que D38 descartó para las expediciones: cada adaptador repetiría el armado de los value objects y la resolución de ids, el hexágono quedaría sin un punto de entrada para el alta, y un segundo adaptador (el cargador del seed de la Fase 5) tendría que duplicarlo.
  - Un solo `/api/equipment` con un discriminador en el cuerpo (`"kind": "vehicle"`) — el cuerpo tendría una patente opcional cuyo carácter obligatorio depende de otro campo, el mapper tendría un condicional por tipo para elegir qué construir y haría falta un error nuevo para un `kind` desconocido, que no es una regla del dominio sino una del adaptador.
  - Una clase por operación (`RegisterStaff`, `RegisterVehicle`, ...) — ver arriba: es la misma discusión que D38 cerró con `TransitionExpedition`.
  - Accesores de categoría y de stock en `Categorized` y `DepletableResource` — ver arriba.
  - Que `ResourceCatalog` expusiera `allVehicles` y `allInstruments` — el catálogo guarda `Equipment` y hoy no conoce los dos tipos concretos; la distinción la necesita la API, así que vive en el caso de uso que la API usa.
  - Validar en los DTOs con Bean Validation para que el id o el nombre vacío no lleguen al dominio — ya está descartado en D49 y en la tabla de patrones no aplicados.
- **Consecuencia:**
  - **El catálogo sigue sin puerto de salida y es deuda.** `ResourceCatalog` es una clase del modelo, no una interfaz de `business.providers`, y en `FieldOpsConfig` es un único bean compartido: registrar un recurso muta estado compartido del contexto, que es el precedente que ya usaba `CreateExpedition` al leerlo. Mientras la persistencia sea en memoria funciona y los tests de API lo acotan levantando un contexto nuevo por test (`@DirtiesContext`, D37). Cuando llegue la persistencia real hace falta un puerto propio (un `ResourceRepository` en `business.providers`, con el catálogo como la vista en memoria que arman las consultas, o un catálogo que reciba el repositorio), porque hoy no hay dónde enganchar la escritura ni la transacción. La señal es el primer adaptador que no sea en memoria.
  - Agregar un tipo de recurso nuevo es un método en `RegisterResources`, su comando, dos métodos en `QueryCatalog` y un controller con sus DTOs y su mapper.
  - Las traducciones que comparten los mappers (período, cantidad, certificaciones y listas ordenadas) quedaron en `Mappings`, package-private en `rest.mappers`, en vez de repetidas en cada uno; `ExpeditionMapper` pasó a usarlo y no cambió de comportamiento.
  - El `amount` de una cantidad viaja como texto (`"10"`): `Quantity` guarda el decimal exacto con los ceros al final removidos, así que un stock de diez unidades serializado como número saldría `1E+1`.

### D53 — El alta de una actividad entra por ids, nombre de tipo y parámetros (Entrega 2, F5, revisa D38 y D44)

- **Origen:** el pendiente que D38 dejó anotado y que D44 repitió como deuda de F1. `ScheduleActivityCommand` era `(ExpeditionId, Activity, TimePeriod)`, y construir una `Activity` pide `(ActivityId, String, TimePeriod, Zone, List<Activity>, ActivityRules)`: una zona, otras actividades y una estrategia de reglas. Un adaptador REST recibe texto e ids y no tiene con qué armar ninguna de las tres, así que hasta este PR la única forma de agendar una actividad era desde Java. El registro de tipos (D44) estaba escrito y no lo usaba nadie fuera de sus tests.
- **Patrón / principio:** caso de uso con comando por id (D38) + Registry (D44) + Decorator pedido con un indicador (D42). El caso de uso es la única frontera que traduce de ids y texto a objetos de dominio.
- **Dónde:** en `business.usecases`, `ScheduleActivityCommand` y `ScheduleActivity`; en `web`, el paquete `adapters.driving.rest`:

  | Pieza | Qué hace |
  |---|---|
  | `ScheduleActivityCommand` | `(expeditionId, activityId, name, timeWindow, scheduledPeriod, zoneId, dependencyIds, typeName, parameters, atNight)`. Los dos períodos y los ids son value objects; el tipo, los parámetros y el indicador son los primitivos que `rulesFor` ya pedía |
  | `ScheduleActivity` | Recibe `ExpeditionRepository`, `ZoneRepository` y `ActivityTypeRegistry`. Resuelve la zona con `Lookups.zone`, cada dependencia con `Lookups.item` sobre el itinerario de la misma expedición, y las reglas con `registry.rulesFor(...)`; después le pasa la `Activity` a la expedición, que aplica las reglas de siempre (D19, D21) |
  | `ActivityController` | `POST /api/expeditions/{expeditionId}/activities`, con `ScheduleActivityRequest` y `ScheduledActivityResponse` |
  | `ActivityTypeController` | `GET /api/activity-types`, sobre `QueryActivityTypes`, con `ActivityTypeResponse` |
  | `rest.dtos` | `ScheduleActivityRequest`, `ScheduledActivityResponse`, `ActivityTypeResponse` y los tres fragmentos de requisitos (`StaffRequirementDto`, `EquipmentRequirementDto`, `SupplyRequirementDto`) |
  | `rest.mappers` | `ActivityMapper` (del pedido al comando y del ítem agendado a la respuesta) y `ActivityTypeMapper` (solo de salida: un tipo no se registra por HTTP) |

  `FieldOpsConfig` le pasa al bean los tres colaboradores, que ya eran beans. `Mappings` sumó `mappedInOrder`, la traducción de una lista cuyo orden es parte de lo que se mandó.
- **Por qué:**
  - **Las tres cosas que faltaban se resuelven en el caso de uso y no en el adaptador.** Es el mismo argumento de D38 y D52: si el controller armara la `Activity`, cada adaptador (el REST, el cargador del seed de la Fase 5) repetiría la búsqueda de la zona, la de las dependencias y la elección de las reglas, y el hexágono quedaría sin un punto de entrada para agendar.
  - **La variante que recibe la `Activity` armada se conserva**, con la firma `schedule(ExpeditionId, Activity, TimePeriod)`. Es el precedente de D38 (`ApproveExpedition.approve`, `FinishExpedition.finish`) y el de `CreateExpedition`, que acepta un comando o un `ExpeditionBuilder`: quien ya tiene el objeto no necesita el registro para volver a armarlo. Reemplazarla habría obligado a reescribir con el registro tests que no son del alta por tipo (los de asignación, candidatos y bloques), y el comando no es el único cliente del dominio.
  - **Una dependencia es una actividad ya agendada en la misma expedición.** Se busca con `Lookups.item`, que mira el itinerario de la expedición del comando. No podría ser de otra forma: es la expedición la que verifica que una dependencia esté agendada y termine antes (D21, D40), y solo ve su propio itinerario. Por eso una dependencia de otra expedición y una que nadie agendó fallan igual, con `NotFoundException` ("no existe actividad agendada con id ..."): desde la expedición que se está planificando, las dos no existen.
  - **El tipo y los parámetros viajan como los pide el registro**, texto y enteros, y el comando no los interpreta. `rulesFor` ya devolvía la regla envuelta en `NightOperationRules` cuando se le pide la variante nocturna (D42, D44), así que el caso de uso no conoce el decorador: pasa el indicador y recibe una `ActivityRules`.
  - **El orden en que se resuelve es el que mejor explica el error:** primero la expedición, después la zona, después las dependencias y al final las reglas. Los tres primeros son 404 (un id que no existe) y el último es 400 (un tipo o un parámetro que no sirven), que es la distinción que ya tenía la tabla de D49; no hizo falta ninguna excepción nueva ni ninguna fila nueva.
  - **La respuesta trae lo que la actividad exige y no sus datos declarados solamente.** Una regla envuelta no dice que es nocturna (D42), así que la API no puede mostrar un `atNight` que venga del dominio: lo que sí puede mostrar es el efecto, que es lo que un cliente necesita después para asignar. Por eso `ScheduledActivityResponse` incluye duración y riesgo estimados, los requisitos de personal, equipamiento y consumibles, las certificaciones que exige a todo el personal y los permisos que exige en su zona, con el nombre exacto que la expedición tiene que conceder (D43). Una nocturna se reconoce por lo que pide (iluminación, la certificación, el permiso, un nivel más de riesgo), que es justo lo que el test de API verifica.
  - **La duración viaja en minutos, en un campo que lo dice (`estimatedDurationMinutes`).** Un `Duration` serializado por Jackson sale como `PT2H` o como un número de segundos según la configuración; el nombre del campo deja la unidad en el contrato y no en una convención del serializador.
  - **La respuesta no trae `Location`.** Lo que se crea es un nodo del itinerario, y el itinerario todavía no tiene endpoint (`GET /api/expeditions/{id}/itinerary` es del tramo siguiente), así que no hay dirección que apuntar. Es el mismo criterio con el que la indisponibilidad responde 204 en D52: no se promete una dirección que no responde. Cuando el endpoint del itinerario exista, el `Location` va ahí.
  - **`GET /api/activity-types` es de solo lectura y no tiene un endpoint por tipo.** El dominio decide qué tipos hay (`withStandardTypes`, D44); la API los publica con su nombre normalizado y los nombres de sus parámetros tal como hay que mandarlos, que es el contrato del cuerpo del alta.
- **Alternativas descartadas:**
  - **Que el adaptador arme la `Activity`** y el comando siga recibiéndola — es lo que D38 y D52 ya descartaron para las expediciones y el catálogo: el adaptador tendría que buscar la zona, buscar las dependencias en el itinerario de la expedición (es decir, cargarla antes que el caso de uso) y conocer el registro de tipos; tres responsabilidades del hexágono afuera, repetidas en el segundo adaptador.
  - **Que el comando reciba la zona y las dependencias ya resueltas** (`Zone` y `List<Activity>`) y solo el tipo por nombre — resuelve la mitad del problema y deja la otra en quien llama: el que tiene que conseguir la `Zone` es el adaptador, y para conseguir las `Activity` de las dependencias tendría que leer el itinerario. Además los comandos de D38 reciben ids por decisión, no por casualidad.
  - **Un endpoint por tipo de actividad** (`POST .../activities/diving`, `POST .../activities/transfer`) — es el mismo antipatrón que D44 descartó con el `switch` por tipo, movido a la URL: sumar un tipo obligaría a agregar un controller o un método, y el parámetro de cada tipo pasaría a ser un campo de un cuerpo distinto. Con un solo endpoint, el tipo es un dato y `GET /api/activity-types` dice cuáles valen. Es lo contrario del caso de D52 con vehículos e instrumentos, donde cada sub-recurso tiene campos propios y obligatorios distintos: acá todos los tipos tienen la misma forma (un nombre y un mapa de parámetros) y la diferencia la valida el dominio.
  - **Un indicador de tipo de variante en vez de `atNight`** (una lista de decoradores a aplicar) — hoy hay una sola variante transversal y el registro la envuelve una sola vez (D42). Una lista abriría la puerta a envolver dos veces, que es justamente la deuda que F1 dejó anotada.
  - **Que la respuesta eche atrás el tipo y el indicador de nocturna que se mandaron** — serían los únicos campos de la respuesta que no salen del dominio sino del pedido, y quedarían desincronizados el día que el itinerario se lea desde el repositorio en vez de desde el pedido que lo creó. Si el dato tiene que viajar, es el dominio el que tiene que guardarlo (D42 lo deja anotado y sigue siendo deuda de F1).
  - **Validar en el DTO que el tipo exista o que los parámetros estén** — ya descartado en D49: el registro falla con `UnknownActivityTypeException` e `InvalidActivityParametersException`, que dicen qué tipos hay y qué parámetro falta, y las dos son 400 por la tabla de errores.
- **Consecuencia:**
  - `ScheduleActivity` depende ahora de tres puertos y no de uno. Es lo que cuesta resolver ids: el mismo esquema de `CreateExpedition` (repositorio, zonas y catálogo).
  - **`ScheduleBlockCommand` sigue recibiendo el `PlannedNode` con las `Activity` ya armadas.** Es el punto 2 de los cambios de dominio de la Fase 5 y el tramo siguiente: el DTO de un bloque es un árbol y cada hoja se traduce con el mismo resolver de esta decisión, así que lo que hoy es privado de `ScheduleActivity` va a tener que compartirse.
  - **Revisado en D55:** esa deuda se resolvió. El resolver (zona, dependencias, reglas) salió de `ScheduleActivity` a una clase compartida, y `ScheduleActivityCommand` pasó a envolver la misma descripción de actividad que usa cada hoja de un bloque.
  - La respuesta de una actividad agendada es la primera forma que se le da a una hoja del itinerario. Cuando se escriba `GET /api/expeditions/{id}/itinerary` conviene que las hojas se serialicen con este mismo `ScheduledActivityResponse`, y lo que falta decidir ahí es el nodo compuesto (D49, pendiente del árbol).
  - **Revisado en D54:** se decidió. Las hojas del itinerario se serializan con este mismo `ScheduledActivityResponse` y el nodo compuesto se resuelve con un Visitor.
  - Agendar sigue sin verificar que una nocturna esté agendada de noche: la actividad se declara nocturna y nada compara su horario con el atardecer. Era un supuesto de F1 y no cambió.

### D54 — El árbol del itinerario se serializa con un Visitor (Entrega 2, F2 y F5, revisa D39 y completa D49)

- **Origen:** punto 3 de los cambios de dominio que la Fase 5 necesita. `ItineraryNode` no expone los hijos ni el tipo de nodo (D39), y la Fase 2 cerró con el criterio de que no haya `instanceof` ni `switch` sobre el tipo de nodo. `GET /api/expeditions/{id}/itinerary` necesita mostrar el árbol: una hoja como actividad y un bloque con sus hijos adentro, sin romper ese criterio.
- **Patrón / principio:** Visitor. Revisa la fila de la tabla de patrones no aplicados que la Fase 2 escribió para este mismo patrón: el argumento que lo descartó decía que las operaciones sobre el árbol son pocas y estables, y ésta es una nueva, de afuera del dominio.
- **Dónde:** `ItineraryNodeVisitor<T>` (en `business.interfaces.itinerary`), con `activity`, `sequence` y `parallel`. `ItineraryNode.accept(ItineraryNodeVisitor<T>)`, implementado en `ItineraryItem` (llama a `activity`) y en `ItineraryBlock` (expone `acceptedChildren`, package-private, para que `SequentialBlock` y `ParallelBlock` llamen a `sequence` y `parallel` con las respuestas de sus hijos ya resueltas). En `web`, `ItineraryMapper` implementa el visitor para armar `ItineraryNodeResponse`, y usa los mismos nombres (`PlannedNodeCommand.SEQUENTIAL`, `PARALLEL`) para el campo `type` de la respuesta.
- **Por qué:**
  - La alternativa que no es Visitor es sumar a `ItineraryNode` lo que le falta: un accesor de hijos y un nombre de tipo. Las dos evitan el `instanceof`, pero difieren en dónde queda el conocimiento de qué tipo de nodo es cada uno. Con hijos y un nombre expuestos, el mapeador tendría que volver a preguntar "¿es una hoja o un bloque, y de qué bloque?" para decidir si el DTO lleva una actividad o una lista de hijos: es el mismo `switch`, corrido una capa afuera del dominio pero no eliminado.
  - Con el Visitor, el que sabe de qué tipo es cada nodo es el nodo mismo: cada uno llama al método de su propio tipo (`ItineraryItem.accept` llama `visitor.activity(this)`, `SequentialBlock.accept` llama `visitor.sequence(this, ...)`), y quien recorre el árbol no pregunta nada. Es la misma idea de D39 (ningún cliente pregunta el tipo de nodo) aplicada a una operación nueva.
  - **Por qué Visitor y no ensanchar la interfaz, si los tipos de nodo siguen siendo tres y estables:** la razón no es que vayan a cambiar, es de qué lado del hexágono vive la necesidad. Mostrar el árbol como JSON es una preocupación del adaptador, no del dominio: ningún caso de uso ni ninguna regla necesita los hijos de un nodo (D39 ya lo dejaba anotado como la razón de no exponerlos). Si se agregan hijos y un nombre a `ItineraryNode`, cualquier cliente del dominio que reciba un nodo pasa a poder recorrer su estructura, aunque no la necesite para nada; con el Visitor, sólo quien implementa uno (hoy, únicamente `ItineraryMapper`) puede observarla. La interfaz del dominio no crece para un caso de uso que no tiene.
  - `ItineraryBlock.acceptedChildren` no publica la lista de hijos: le entrega al visitor las respuestas ya calculadas (el mismo patrón ya usado por `planKeepingStructure`, que tampoco expone los hijos sino que entrega los planes ya armados). La lista de `ItineraryNode` sigue sin un getter público.
  - El nombre del tipo de bloque (`"sequential"`, `"parallel"`) no se inventa en el mapeador: son las mismas constantes que D55 usa para nombrar un bloque en el comando de alta, así que la respuesta de lectura y el pedido de escritura comparten vocabulario.
- **Alternativas descartadas:**
  - Agregar `children()` y un nombre de tipo a `ItineraryNode` — ver el "por qué": no elimina el condicional, lo corre al mapeador, y expone la estructura a cualquier cliente del dominio sin que ninguno la necesite.
  - `instanceof` o `switch` directo sobre el tipo de nodo en el mapeador — es exactamente lo que el cierre de F2 prohibió.
  - Sellar `ItineraryNode` (`sealed interface`) y usar `switch` con *pattern matching* sobre los tres subtipos — evita el `instanceof` suelto pero sigue siendo un `switch` sobre el tipo, ahora fijado a nivel de lenguaje: agregar un cuarto tipo de bloque obligaría a tocar todos los `switch` existentes igual que agregar un método al Visitor obliga a tocar todos los visitors, sin ninguna ventaja a cambio, y además acoplaría `ItineraryNode` a sus tres implementaciones actuales desde la propia interfaz.
- **Consecuencia:**
  - Agregar un tipo de nodo nuevo (una hoja distinta, un tercer tipo de bloque) es agregar un método a `ItineraryNodeVisitor` y a cada visitor existente (hoy, uno). Es el costo conocido del patrón, y es simétrico con el de D39: una forma de combinar nueva ya costaba una clase y una entrada en `PlannedNode`.
  - `ItineraryNode` ganó un único método (`accept`) y no un conjunto de accesores de estructura; la invariante de D39 ("la interfaz no expone los hijos de un nodo") se mantiene para todo cliente que no sea un visitor.
  - La fila de Visitor en "Patrones que decidimos no aplicar" se actualiza: ya no es un patrón descartado en general, sino uno que no se usa para las operaciones del dominio sobre el árbol (duración, riesgo, consumo, hojas, paralelismo, plan), que siguen siendo pocas y estables y van como métodos directos; se usa para la única operación que no es del dominio, mostrar el árbol hacia afuera.

### D55 — El alta de un bloque entra por un árbol de descripciones, con un resolver compartido con el alta de actividad (Entrega 2, F2 y F5, revisa D38, D39 y D53)

- **Origen:** punto 2 de los cambios de dominio que la Fase 5 necesita. `ScheduleBlockCommand` recibía `(ExpeditionId, PlannedNode)`, y `PlannedNode.activity(...)` recibe una `Activity` ya construida: el mismo problema que D53 resolvió para una actividad suelta, un nivel más arriba, porque el adaptador REST tampoco puede construir las actividades de un árbol.
- **Patrón / principio:** el mismo esquema de D39 (una fábrica por forma de combinar, cada una guardando la función que construye) aplicado a descripciones en vez de a nodos ya resueltos; el resolver de D53 (zona, dependencias, reglas) extraído a una clase compartida.
- **Dónde:** en `business.usecases`: `PlannedActivityCommand` (la descripción de una hoja: id, nombre, ventana, horario, zona, dependencias, tipo, parámetros, nocturna — los mismos nueve datos que antes tenía `ScheduleActivityCommand`), `PlannedNodeCommand` (el árbol: `activity(...)`, `sequential(...)`, `parallel(...)` y `block(String, List)` para un nombre de tipo dinámico), y `PlannedActivities` (el resolver, package-private, con un `Map<ActivityId, Activity>` de lo que ya construyó en este pedido). `ScheduleActivityCommand` pasó a ser `(expeditionId, PlannedActivityCommand)` y `ScheduleBlockCommand`, `(expeditionId, PlannedNodeCommand)`. `ScheduleActivity` y `ScheduleBlock` reciben ambos `ExpeditionRepository`, `ZoneRepository` y `ActivityTypeRegistry`, y los dos construyen un `PlannedActivities` por pedido.
- **Por qué:**
  - **El resolver se comparte y no se duplica.** D53 ya dejó anotado que esto iba a hacer falta: "lo que hoy es privado de `ScheduleActivity` va a tener que compartirse". `PlannedActivities.of(PlannedActivityCommand)` hace exactamente lo que hacía el método privado de `ScheduleActivity`: busca la zona con `Lookups.zone`, las reglas con `ActivityTypeRegistry.rulesFor` y cada dependencia, y es la única pieza nueva: antes de buscar una dependencia en el itinerario de la expedición, la busca entre lo que el mismo pedido ya construyó.
  - **¿Una hoja puede depender de un hermano del mismo bloque? Sí, se resuelve dentro del mismo pedido.** Es el punto que el plan pedía decidir y justificar. Dos hojas de un mismo bloque todavía no están agendadas mientras se arma el árbol, así que `Itinerary.itemFor` (lo que usa D53 para una dependencia) no las encuentra; la pregunta es si eso tiene que ser un 404 o si el pedido tiene que resolverlo él mismo.
    - D40 ya dice que el orden declarado de un secuencial expresa la precedencia, y que un bloque se agenda todo o nada después de construirse entero: una hoja que depende de una hermana anterior del mismo secuencial no le pide nada a la expedición que no sepa ya, porque el período de la hermana ya está fijado en el mismo pedido.
    - D40 también dice (y D41 lo repite para los recursos) que dos ramas de un mismo paralelo no pueden depender entre sí, porque el bloque declara que corren a la vez. Esa regla no cambia: sigue viviendo en `Itinerary.add` (`requireNotInParallel`), que se evalúa cuando el nodo completo —ya con todas sus `Activity` construidas por `PlannedActivities`— se agrega al itinerario real. La decisión de este PR es sólo sobre **cómo se busca** una dependencia antes de ese punto, no sobre si se admite entre ramas paralelas.
    - La alternativa de rechazar toda dependencia hacia un hermano con 404 ("todavía no agendado") no tiene respaldo en el dominio: el mismo par de actividades, pedido en dos llamadas separadas a `POST /activities` en vez de en un único `POST /blocks`, se aceptaría sin problema. Que el alta por bloque sea más restrictiva que la suma de altas sueltas no refleja ninguna regla de negocio, sólo una limitación de implementación (que `Itinerary.itemFor` no ve lo que todavía no se agendó).
    - La dirección contraria (depender de un hermano que viene **después** en el árbol) no se admite, y no por una regla nueva: `PlannedActivities` construye las hojas en el orden en que aparecen en el árbol (como D39 ya hace con `PlannedNode`), así que al construir una hoja ninguna posterior existe todavía. Pedir una dependencia hacia adelante falla con `NotFoundException`, igual que una dependencia que nadie agendó: antes de que el itinerario pueda verificar que termine antes (D21), ni siquiera existe para resolverse.
  - **`PlannedNodeCommand` repite la forma de `PlannedNode` (D39) y no la reutiliza.** Cada fábrica (`activity`, `sequential`, `parallel`) guarda una función que, dado el resolver, arma el `PlannedNode` real. Es la misma idea (una fábrica por forma de combinar, sin un modo que consultar) aplicada un nivel antes: `PlannedNode.activity(...)` ya pide una `Activity` armada, que es justo lo que el adaptador no tiene, así que no puede construirse uno hasta que el resolver corrió.
  - **El nombre del tipo de bloque (`sequential`/`parallel`) es un dato y no un método por tipo**, con el mismo criterio que D53 usa para el tipo de actividad: un `Map<String, Function<...>>` en `PlannedNodeCommand.block(String, List)`, el nombre normalizado como `ActivityTypeName` (D34), y una lista cerrada de dos nombres. Un nombre que no es ninguno de los dos falla con `InvalidScheduleException` (400, ya mapeado): es un plan mal formado, no un recurso que falta.
  - El árbol se agenda todo o nada igual que antes (D40): `PlannedActivities` sólo construye objetos `Activity` en memoria; nada se agrega al itinerario real hasta que `Expedition.schedule` recibe el `PlannedNode` completo y corre sus propias verificaciones.
- **Alternativas descartadas:**
  - **Rechazar toda dependencia hacia un hermano del mismo bloque con 404** — es la lectura más simple (no hace falta el mapa de construidos), pero es más estricta que agendar las mismas actividades sueltas una por una, sin que ninguna regla de D21 o D40 lo pida.
  - **Resolución en dos pasadas** (construir todas las hojas sin dependencias y "parchearlas" después) — `Activity.dependencies` es `final` a propósito (ver "Supuestos": así se garantiza que no hay ciclos, por construcción); permitir una dependencia tardía rompería esa garantía para ganar nada, porque construir en el orden del árbol ya alcanza.
  - **Mantener el resolver privado de `ScheduleActivity` y repetirlo en `ScheduleBlock`** — es lo que D53 ya advirtió que no convenía: dos copias de la misma búsqueda de zona, dependencias y reglas que podrían divergir (por ejemplo, si sólo una tuviera el mapa de construidos del mismo pedido).
- **Consecuencia:**
  - `ScheduleActivity` y `ScheduleBlock` quedan con la misma firma de colaboradores (`ExpeditionRepository`, `ZoneRepository`, `ActivityTypeRegistry`) y el mismo resolver por detrás; una corrección en cómo se resuelve una zona, una dependencia o un tipo se escribe una sola vez.
  - Una dependencia hacia un hermano anterior del mismo bloque se ve igual que una hacia una actividad ya agendada: el mismo `ScheduledActivityResponse.dependencyIds`, sin marca especial.
  - Agregar un tipo de bloque nuevo es una entrada en el mapa de `PlannedNodeCommand` y una fábrica en `PlannedNode` (ya era así desde D39); no agrega costo esta decisión.

### D56 — Asignación de recursos y candidatos: sin cambios de dominio, y lo asignado se suma a la respuesta del itinerario (Entrega 2, F2 y F5, revisa D49 y D52)

- **Origen:** cuarto tramo del PR 5b. A diferencia de los tres anteriores, `AssignResources` y `FindAssignmentCandidates` ya eran casos de uso completos y ya eran beans de `FieldOpsConfig` desde la Fase 1b: no hizo falta ningún cambio de dominio, solo el adaptador. Quedaban dos preguntas abiertas que el plan dejó para este tramo: qué forma tiene el DTO de candidatos, y si lo asignado se muestra en `GET /api/expeditions/{id}/itinerary` o queda pendiente (`ScheduledActivityResponse` no lo mostraba).
- **Patrón / principio:** el mismo de D49 (adaptador driving sin lógica propia); ninguno nuevo, porque no hay nada que resolver por id (D38) ni ningún tipo que registrar (D44): los tres métodos de `AssignResources` ya reciben `ExpeditionId`, `ActivityId` y `ResourceId`.
- **Dónde:** en `web`, `adapters.driving.rest.AssignmentController`, con `@RequestMapping("/api/expeditions/{expeditionId}/activities/{activityId}")`:

  | Endpoint | Caso de uso | Responde |
  |---|---|---|
  | `POST .../staff`, `.../equipment`, `.../supplies` | `AssignResources.assignStaff/assignEquipment/assignSupply` | 204, sin cuerpo |
  | `GET .../candidates` | `FindAssignmentCandidates.candidatesFor` | 200 con `AssignmentCandidatesResponse` |

  DTOs nuevos: `AssignResourceRequest` (un `resourceId`, compartido por los tres `POST`), `AssignmentCandidatesResponse` (tres listas, una por tipo de requisito) con `StaffCandidatesDto`, `EquipmentCandidatesDto` y `SupplyCandidatesDto`, y el mapper `AssignmentCandidatesMapper`. `ScheduledActivityResponse` sumó `assignedStaffIds`, `assignedEquipmentIds` y `assignedSupplyIds`, y `ActivityMapper.toResponse` los completa desde `ItineraryItem.getAssignedStaff/Equipment/Supplies` (ya existían en el dominio, sin tocarlo). Los tres `toDto` privados de `ActivityMapper` que traducen un requisito pasaron a package-private para que `AssignmentCandidatesMapper` los reuse.
- **Por qué:**
  - **Los tres `POST` responden 204 y no 201.** `AssignResources.assignStaff/assignEquipment/assignSupply` devuelven `void`: no hay un objeto nuevo que el adaptador pueda devolver, ni una asignación tiene su propio id al que apuntar con `Location`. Es el mismo criterio que D52 usa para la indisponibilidad de un recurso (`POST .../unavailabilities` también responde 204): el efecto se observa leyendo el recurso que cambió, no un objeto nuevo.
  - **Lo asignado se suma a `ScheduledActivityResponse` en vez de quedar pendiente.** Era la decisión que el plan dejaba abierta. Se decidió sumarlo porque, sin esto, una asignación no tendría ninguna forma honesta de observarse por HTTP: los tres `POST` responden 204 sin cuerpo y no existe un `GET` de una asignación en particular (no tiene id propio, el mismo motivo por el que no tiene `Location`). La validación tampoco sirve como sustituto: `MissingResourcesValidation` cuenta personal asignado por cantidad y no por categoría, así que asignar cualquier persona la "resuelve" aunque no cumpla el requisito (ver más abajo), y el equipo o el consumible recién asignado sigue apareciendo entre los candidatos de `GET .../candidates` porque las reservas que lo sacarían de la disponibilidad se crean al aprobar (`ResourceReservations`), no al asignar. La única forma de verificar que una asignación tuvo efecto es leerla de vuelta en el itinerario. El dominio ya tenía los tres accesores (`ItineraryItem.getAssignedStaff/Equipment/Supplies`) desde la Fase 2, así que mostrarlos es traducción pura, sin ninguna regla nueva en `domain`.
  - **Se muestran ids y no los recursos completos**, con el mismo criterio que `dependencyIds`: quien necesita el nombre o la categoría de un recurso ya tiene `GET /api/staff/{id}`, `/api/vehicles/{id}`, etc.; repetir el objeto entero en cada actividad agendada duplicaría lo que el catálogo ya expone.
  - **El DTO de candidatos tiene una entrada por requisito, pareando el requisito con sus candidatos**, y no una lista plana de recursos. Es la forma que describe `AssignmentCandidates` en el dominio (`AssignmentCandidates.For<R, C>`): cada requisito de personal, equipo o consumible de la actividad con lo que el catálogo puede ofrecerle. Los fragmentos de requisito (`StaffRequirementDto`, `EquipmentRequirementDto`, `SupplyRequirementDto`) son los mismos que D53 ya definió para `ScheduledActivityResponse`: la forma coincidía y repetirla hubiera sido la misma información dos veces.
  - **El equipo se separa en `vehicles` e `instruments`, igual que `GET /api/equipment` (D52).** Un candidato que cumple la categoría de un `ReusableRequirement` puede ser cualquiera de los dos (`ResourceCatalog` los guarda juntos como `Equipment`), y no tienen la misma forma: el vehículo tiene patente. Separarlos evita un discriminador y una patente que a veces es `null`, el mismo argumento de D52.
  - **Los candidatos de consumibles castean a `Depletable`.** `ResourceCatalog.suppliesCovering` devuelve `DepletableResource`, la interfaz, pero `Depletable` es la única clase que la implementa (igual que `Lookups.depletable` ya asume al resolver un id); `SupplyMapper.toResponse` pide la clase concreta porque ahí vive la categoría y el stock.
  - **Es un controller nuevo (`AssignmentController`) y no un agregado a `ActivityController`.** Agrupa su propio sub-recurso (`.../activities/{activityId}/...`), con una base de ruta distinta a la de `ActivityController` (`.../activities`, sin el id). D49 ya fija un controller por recurso; mezclar los dos habría dejado un controller respondiendo a dos formas de ruta por razones distintas (crear una actividad nueva contra operar sobre una ya agendada).
  - **El tercer criterio de aceptación de F2 (conflicto de recursos entre ramas paralelas) ya tiene test de API.** `ExpeditionPlanApiTest` anotaba que esperaba este tramo (D54); con `AssignmentController` expuesto, `theValidationReportsAResourceConflictBetweenParallelActivitiesOfTheSameBlock` asigna la misma persona a dos actividades de un bloque paralelo y verifica el crítico de `resource-overlap` por `GET /validation`. Revisa la nota de F2 en D49 y la entrada de la tabla de criterios de F2.
- **Alternativas descartadas:**
  - **Dejar `ScheduledActivityResponse` sin tocar y la asignación sin forma de observarse por HTTP más allá de la validación** — descartada por lo explicado arriba: sin reservas inmediatas y con `MissingResourcesValidation` contando por cantidad y no por categoría para el personal, no hay una señal confiable de que una asignación concreta tuvo efecto.
  - **Devolver el recurso completo en vez de su id en las listas de asignados** — repetiría en cada actividad agendada lo que ya expone el recurso en su propio sub-recurso del catálogo, igual que D53 ya decidió para `dependencyIds`.
  - **Una sola lista de equipo en el DTO de candidatos, con un discriminador de tipo** — mismo argumento que D52 descartó para `/api/equipment`.
  - **Agregar los tres `POST` y el `GET` de candidatos a `ActivityController`** — mezclaría dos bases de ruta (`/activities` y `/activities/{activityId}`) en un mismo controller sin necesidad.
  - **Un `GET` propio para leer una asignación** — no hay un id de asignación al que apuntar (una asignación no es una entidad, es un recurso agregado a un conjunto); es el mismo motivo por el que D52 no le da `Location` a la indisponibilidad.
- **Consecuencia:**
  - `ScheduledActivityResponse` crece de doce a quince campos. No rompe ningún test existente porque ninguno compara el cuerpo completo: todos verifican campos puntuales con `jsonPath`.
  - Agregar un cuarto tipo de recurso asignable sería un método más en `AssignResources` (fuera de este PR, ya que no hizo falta), un endpoint más en `AssignmentController`, una lista más en `AssignmentCandidatesResponse` y tres campos más en `ScheduledActivityResponse`.
  - Sigue habiendo una asimetría conocida y aceptada: assignar un recurso que no cumple un requisito no falla (el dominio no lo valida al asignar, solo al validar la expedición con `ApprovalPolicy`), y este tramo no la cambia porque no es un cambio de dominio que el plan haya pedido. Queda demostrado con un test (`AssignmentApiTest.assigningEquipmentOfTheWrongCategorySucceedsAndTheMismatchShowsUpOnlyAtValidation`) en vez de quedar como supuesto implícito.

### D57 — El ciclo de vida de una expedición: estados, aprobación, cierre, seguimiento e informe, sin cambios de dominio (Entrega 2, F5, cierra el PR 5b)

- **Origen:** quinto y último tramo del PR 5b. Como D56, `TransitionExpedition`, `ApproveExpedition`, `FinishExpedition`, `TrackActivity` y `ReportExpedition` ya eran casos de uso completos y ya eran beans de `FieldOpsConfig` desde fases anteriores: no hizo falta ningún caso de uso nuevo, solo el adaptador.
- **Patrón / principio:** el mismo de D49 y D56 (adaptador driving sin lógica propia) + la simetría de D32: un hallazgo se identifica por su regla y su sujeto, así que aceptarlo es copiar esa identidad.
- **Dónde:** en `web`, `adapters.driving.rest.ExpeditionLifecycleController` (`@RequestMapping("/api/expeditions/{expeditionId}")`) con los siete sub-recursos de transición y `GET .../report`; `ActivityTrackingController` (`@RequestMapping("/api/expeditions/{expeditionId}/activities/{activityId}")`, la misma base que `AssignmentController`) con `/start`, `/completion` y `/observations`.

  | Endpoint | Caso de uso | Responde |
  |---|---|---|
  | `POST .../review`, `/draft`, `/execution`, `/suspension`, `/resumption` | `TransitionExpedition` | 204 |
  | `POST .../approval` | `ApproveExpedition.approve(id, acceptances)` | 204 |
  | `POST .../completion` | `FinishExpedition.finish(id)` | 204 |
  | `GET .../report` | `ReportExpedition.report(id)` | 200 con `ExpeditionReportResponse` |
  | `POST .../activities/{activityId}/start`, `/completion`, `/observations` | `TrackActivity` | 204 |

  DTOs nuevos: `ApprovalRequest`, `WarningAcceptanceRequest`, `StartActivityRequest`, `FinishActivityRequest`, `ObservationRequest`, `ExpeditionReportResponse`, `IncidentDto` y `AcceptedWarningDto`; mappers `ApprovalMapper` y `ReportMapper`. `ValidationMapper` sumó `toSubject` (la inversa de su propio `toDto(ValidationSubject)`) y `toDto(ValidationResult)` pasó de privado a package-private; `EstimateMapper` sumó `toConsumptionDtos`, extraído de `toResponse`, package-private.
- **Por qué:**
  - **Las ocho transiciones (incluida la aprobación y el cierre) responden 204 y no devuelven nada propio.** Ninguna crea un recurso con dirección propia (D49, D52): lo que cambió se lee de vuelta con `GET /api/expeditions/{id}` (el estado) o, para la aprobación y el cierre, con `GET .../report`. Es el mismo criterio que D52 y D56 ya aplicaron a la indisponibilidad y a la asignación.
  - **El cuerpo de la aprobación se construye copiando la respuesta de la validación.** `WarningAcceptanceRequest(rule, subject, responsibleId, justification)` repite exactamente los dos primeros campos de `ValidationFindingDto` (`rule: String`, `subject: ValidationSubjectDto`), así que el guion de la demo es: `GET .../validation` devuelve una advertencia, se copian su `rule` y su `subject` tal cual, se agregan quién acepta y por qué, y eso ya es un `WarningAcceptanceRequest` válido. `ApprovalMapper.toAcceptance` reconstruye el `WarningAcceptance` del dominio con `ValidationMapper.toSubject`, la función inversa de la que ya traducía un `ValidationSubject` a DTO — un value object nuevo no hizo falta porque D32 ya había identificado un hallazgo por su regla y su sujeto, y ese es justamente el par que viaja. `ExpeditionLifecycleApiTest.approvalAcceptsAWarningCopiedFromTheValidationResponseAndTheReportShowsItAccepted` hace ese recorrido completo: valida, lee la advertencia de `GET .../validation`, la acepta con una justificación y aprueba.
  - **Una transición ilegal es 409 y una regla de aprobación que no se cumple es 422, la misma distinción que D49 ya fijaba.** `InvalidStatusTransitionException` (la expedición no está en el estado que la operación exige, D20) es un conflicto de protocolo; `ExpeditionNotApprovableException` (hay una crítica abierta) y `UnacceptedWarningException` (una advertencia que la expedición levanta no fue aceptada) son 422 porque lo que falla es una regla de negocio sobre un pedido bien formado. Aceptar una advertencia con una regla y un sujeto que la expedición no levanta ahora mismo es 404: `ApproveExpedition.accepted` ya resolvía esto con `Lookups`-style `NotFoundException` desde la Fase 1b, sin que este tramo tocara nada.
  - **La deuda conocida de D49 sobre `ExpeditionNotApprovableException` queda como estaba, a propósito.** D49 anotó que la lista de críticas viaja en el mensaje y no como un arreglo propio en el cuerpo, y que se resolvía en este PR. Se decidió no tocarlo: hacerlo exigiría que la excepción de dominio guarde los `ValidationResult` con un getter nuevo, y el plan de este tramo pedía expresamente no tocar el dominio salvo necesidad real. El mensaje ya enumera cada crítica (`"The expedition cannot be approved because of: %s; %s"`), así que el cuerpo de un 422 **ya dice** cuáles son las críticas, aunque sea como texto y no como lista estructurada; `ExpeditionLifecycleApiTest.approvingWithACriticalStillOpenFailsAndTheBodyListsIt` lo verifica buscando cada mensaje dentro de `detail`. La deuda sigue abierta para quien la necesite estructurada (ej. un cliente que quiera iterarlas), y no para este guion de demo.
  - **El seguimiento de una actividad (`start`, `finish`, `observe`) recibe la fecha y, al terminar, el resultado, en el cuerpo y no en query params**, con el mismo criterio que cualquier otro `POST` de este adaptador: son datos, no identificadores de recurso. `Observation(note, recordedAt)` del dominio se construye directo en el controller, igual que `ResourceId` o `ExpeditionId` en los demás controllers — no hizo falta un mapper para una traducción de dos campos sin lógica.
  - **El informe reusa `EstimateResponse` y `QuantityDto` en vez de inventar su propia forma de mostrar un consumo.** `ExpeditionReport.estimate()` es el mismo `ExpeditionEstimate` que `EstimateMapper.toResponse` ya traduce; `ExpeditionReport.actualConsumption()` es el mismo `Map<ResourceCategory, Quantity>` que `estimatedConsumption`, así que `EstimateMapper.toConsumptionDtos` se extrajo para que las dos listas (lo estimado y lo realmente consumido) se lean con la misma forma (`List<SupplyRequirementDto>`, ordenada por categoría) y la misma unidad de texto exacto de D52 para `QuantityDto`. `ReportMapper` solo arma el resto: el nombre, el estado (el enum se reusa, D49), las dos cantidades de actividades y dos traducciones nuevas.
  - **`IncidentDto` es la primera vez que un incidente sale por HTTP**, y queda deliberadamente simple (los mismos campos que `Incident`, con el recurso y el retraso opcionales como `null`) porque F3 todavía no tiene su propio endpoint: cuando ese tramo llegue, reusa este DTO en vez de inventar el suyo, el mismo patrón por el que D53 dejó `ScheduledActivityResponse` listo para que D54 lo reusara en el itinerario.
  - **`AcceptedWarningDto` reusa `ValidationFindingDto` para el campo `warning`**, cerrando el círculo que la aprobación abrió: el mismo finding que se copió de la validación para aceptarlo es el que el informe muestra de vuelta, con quién lo aceptó y por qué. `ValidationMapper.toDto(ValidationResult)` pasó de privado a package-private para que `ReportMapper` lo reuse, el mismo criterio con el que D56 abrió los `toDto` de requisitos de `ActivityMapper`.
  - **`ActivityTrackingController` es un controller aparte y no un agregado a `AssignmentController`**, aunque comparten la misma base de ruta (`.../activities/{activityId}`): son dos concerns distintos sobre el mismo sub-recurso (asignar recursos contra seguir la ejecución), igual que `ActivityController` y `AssignmentController` ya están separados pese a compartir prefijo.
  - **`GET .../report` vive en `ExpeditionLifecycleController` y no en un controller propio.** El informe es la lectura que cierra el ciclo de vida (lo que se aprobó, lo que se ejecutó, lo que se aceptó), así que queda con las transiciones en vez de sumar un controller de una sola operación.
- **Alternativas descartadas:**
  - **Devolver el estado nuevo o el informe completo en el cuerpo de cada transición** — ninguna transición necesita devolver más que el éxito; el estado ya se puede leer con `GET /api/expeditions/{id}` y duplicarlo en cada respuesta repetiría información sin agregar nada que el cliente no tenga ya.
  - **Que `ExpeditionNotApprovableException` exponga los `ValidationResult` y el handler los liste estructurados** — es la lectura literal de la nota de D49, pero exige un cambio de dominio (un getter nuevo en una excepción, o que guarde la lista en vez de solo el mensaje) que el alcance de este tramo pedía evitar salvo necesidad real; el mensaje ya satisface el criterio de aceptación ("el cuerpo dice cuáles son las críticas") sin tocar `domain`. Se deja anotada como la extensión natural si algún cliente necesita iterarlas.
  - **Un value object `WarningAcceptanceKey` o similar para el par regla-sujeto** — D32 ya lo resolvió: el par vive en `ValidationResult` (`rule`, `subject`) y en `WarningAcceptance` del lado del dominio; agregar un tercer tipo solo para el DTO hubiera sido una capa más sin una regla propia.
  - **Mapear `Observation`, `at` y `result` con un `TrackingMapper`** — son traducciones de uno o dos campos primitivos sin ninguna regla, el mismo caso por el que otros controllers construyen sus value objects directo (`new ResourceId(...)` en `AssignmentController`). Un mapper de una línea por método no agrega nada que el constructor del value object no exprese ya.
- **Consecuencia:**
  - El PR 5b queda completo salvo F3 (registrar incidente, consultar propuestas y decidir), que es el tramo siguiente y la última pieza de "Endpoints pendientes de F1, F2 y F3" que la Fase 5 absorbió.
  - `IncidentDto` y el criterio de reuso de `ValidationFindingDto`/`EstimateResponse` quedan como el molde que F3 puede seguir: un incidente que entra por `POST /api/expeditions/{id}/incidents` puede responder con el mismo `IncidentDto`, y una propuesta puede mostrar sus opciones con las mismas piezas (`ResourceCategory`, `QuantityDto`) que ya existen.
  - Agregar una sexta transición (si el enunciado alguna vez la pidiera) sería un método en `TransitionExpedition` y un endpoint en `ExpeditionLifecycleController`, sin tocar los demás.

## Funcionalidades nuevas de la Entrega 2

### F1 — Actividades nocturnas

- **Decisiones:** D42, D43, D44, D53.
- **API:** dos endpoints, con el alta de actividad por tipo que D53 habilitó.

  | Endpoint | Caso de uso | Qué devuelve |
  |---|---|---|
  | `POST /api/expeditions/{id}/activities` | `ScheduleActivity.schedule(command)` | La actividad agendada con lo que sus reglas exigen: duración y riesgo estimados, requisitos de personal, equipamiento y consumibles, certificaciones para todo el personal y permisos en su zona. Una nocturna se pide con `"atNight": true` y se reconoce por lo que exige, porque el dominio no dice que lo es (D42) |
  | `GET /api/activity-types` | `QueryActivityTypes.list()` | Los seis tipos con el nombre normalizado y los nombres de sus parámetros. Es lo que le dice al cliente qué puede pedir en el alta |
- **Clases agregadas:**
  - `business.rules.ActivityRulesDecorator` — base abstracta de los decoradores de `ActivityRules`: delega todos los métodos en la regla que envuelve.
  - `business.rules.NightOperationRules` — la variante nocturna: suma iluminación, certificación para todo el personal y permiso de la zona, y sube un nivel el riesgo.
  - `business.rules.DroneSurveyRules`, `WildlifeObservationRules`, `WaterQualityMeasurementRules` y `TransferRules` — los cuatro tipos de actividad que faltaban.
  - `business.interfaces.activities.ActivityType` — un tipo de actividad: su nombre, sus parámetros y cómo crear sus reglas.
  - `business.rules.SingleParameterActivityType` — implementación de `ActivityType` para los tipos que toman un entero.
  - `business.rules.ActivityTypeRegistry` — del nombre de un tipo y sus parámetros a una `ActivityRules`, con la variante nocturna si se pide.
  - `business.models.activities.ActivityTypeName` y `ActivityParameters` — el nombre normalizado de un tipo y los parámetros con que se lo pide.
  - `business.exceptions.UnknownActivityTypeException` e `InvalidActivityParametersException` — un tipo que no existe y un parámetro que falta o no sirve.
  - `business.usecases.QueryActivityTypes` — lista los tipos disponibles con sus parámetros.
  - En `web`, con el alta por tipo (D53): `adapters.driving.rest.ActivityController` y `ActivityTypeController`; en `rest.dtos`, `ScheduleActivityRequest`, `ScheduledActivityResponse`, `ActivityTypeResponse`, `StaffRequirementDto`, `EquipmentRequirementDto` y `SupplyRequirementDto`; en `rest.mappers`, `ActivityMapper` y `ActivityTypeMapper`.
- **Clases modificadas:**
  - `ActivityRules` — suma `requiredCertificationsForAllStaff()` y `requiredPermitsIn(Zone)`, con respuesta vacía por defecto.
  - `Activity` — expone las dos preguntas nuevas (`requiredCertificationsForAllStaff`, `requiredPermits`) y responde `admits(persona)` y `missingPermits(concedidos)`.
  - `RiskLevel` — suma `EXTREME` y `next()`.
  - `Zone` — suma `specialPermitFor(operación)`.
  - `MissingCertificationsValidation` — además de los requisitos de personal, reporta a cada persona asignada que la actividad no admite.
  - `MissingPermitsValidation` — además de los permisos de las zonas, reporta los de cada actividad pendiente.
  - `FindAssignmentCandidates` — ofrece solo al personal que la actividad admite.
  - `DivingRules` y `SampleCollectionRules` — suman la constante `TYPE`.
  - `ScheduleActivityCommand` — pasa a recibir ids, el nombre del tipo, sus parámetros y el indicador de nocturna en vez de la `Activity` armada (D53).
  - `ScheduleActivity` — resuelve la zona, las dependencias y las reglas, y recibe `ZoneRepository` y `ActivityTypeRegistry` además del repositorio (D53). Conserva la variante que recibe la `Activity`.
  - `FieldOpsConfig` (módulo `web`) — expone `ActivityTypeRegistry` y `QueryActivityTypes` como beans (D37), y le pasa los tres colaboradores a `ScheduleActivity` (D53).
  - `rest.mappers.Mappings` (módulo `web`) — suma `mappedInOrder`, para las listas cuyo orden es parte de lo que se mandó (los ids de las dependencias).
  - No se modificaron `ExpeditionReporter`, `ExpeditionEstimator`, `RiskToleranceValidation`, `MissingResourcesValidation`, `ApprovalPolicy` ni `ItineraryItem`.
- **Refactorizaciones realizadas:**
  - `MissingPermitsValidation` separó en dos métodos lo que reporta por zona y lo que reporta por actividad, con el texto del mensaje en una constante que comparten. Hizo falta para sumar el segundo origen sin duplicar el mensaje.
  - En `MissingCertificationsValidation`, el método que arma la lista de nombres pasó de recibir un `StaffRequirement` a recibir el conjunto de certificaciones, para usarlo también con las que se exigen a todo el personal.
  - No hubo que reestructurar nada más: los tests de la Entrega 1 y de F2 pasan sin cambios.
- **Deuda técnica que se decidió no resolver:**
  - **Nada impide envolver dos veces una regla fuera del registro.** `new NightOperationRules(new NightOperationRules(regla))` sube el riesgo dos niveles y pide la iluminación dos veces. Se aceptó porque el único camino que arma la variante a partir de un pedido es el registro, que envuelve una vez. Habría que resolverlo si aparecen más decoradores o si otro código empieza a armar reglas envueltas por su cuenta.
  - **Una regla base que ya pida iluminación la pediría dos veces.** Los dos requisitos se evalúan por separado, así que un mismo equipo cubre ambos (el mismo límite que ya tiene el personal, ver supuestos). Hoy ningún tipo base pide `Lighting`.
  - **Una actividad no dice que es nocturna, ni de qué tipo es.** Es lo que evita los condicionales, pero la respuesta de la API no puede mostrarlo a partir del dominio. Si hace falta, el dato viaja en la respuesta o en una descripción que las reglas publiquen, no en un `isNight()`. El alta por tipo no lo resolvió: la respuesta muestra lo que la actividad exige, no de qué tipo es ni si es nocturna (D53).
  - **El permiso especial se identifica por una convención de nombre.** Alcanza mientras un permiso sea solo un nombre (D43). Habría que resolverlo si los permisos pasan a tener vigencia o emisor.
  - **Los parámetros de un tipo son enteros.** Alcanza para los seis tipos; un tipo con otro dato obliga a ampliar `ActivityParameters` (D44).
  - **El horario de una actividad nocturna no se verifica.** La actividad se declara nocturna y nada comprueba que su horario agendado sea de noche (ver supuestos).
- **Criterios de aceptación:**
  - Puede crearse una expedición con al menos una actividad nocturna — `CreateExpeditionTest.createsAnExpeditionWithANightActivity`, con la actividad armada por el registro.
  - La validación exige la certificación — `MissingCertificationsValidationTest.flagsEachPersonAssignedToANightActivityWithoutTheNightCertification` y, para la persona de más con el cupo cubierto, `flagsAnExtraPersonWithoutTheNightCertificationEvenIfTheHeadcountIsCovered`.
  - La validación exige el permiso — `MissingPermitsValidationTest.flagsANightActivityWhoseZonePermitWasNotGranted`; que el permiso de otra zona no lo cubre, `theNightPermitOfAnotherZoneDoesNotCoverTheActivity`.
  - Las dos faltas impiden aprobar, y con todo en regla la actividad pasa las siete reglas — `ApprovalPolicyTest.aNightActivityWithoutItsCertificationAndItsPermitPreventsTheApproval` y `aNightActivityWithItsCertificationItsPermitAndLightingPassesTheMandatoryRules`.
  - La iluminación que falta la reporta la regla de recursos sin haberla tocado — `MissingResourcesValidationTest.aNightActivityIsMissingEquipmentUntilItGetsLighting`.
  - La estimación de riesgo refleja el factor nocturno — `ExpeditionEstimatorTest.theRiskEstimateReflectsTheNightFactor` y `anActivityThatIsAlreadyHighRiskByDayIsExtremeAtNight`; por cada nivel base, incluido el máximo, el parametrizado `NightOperationRulesTest.raisesTheRiskOfTheBaseRulesOneLevelAndStaysAtTheMaximum`.
  - Los informes existentes incluyen la variante sin condicionales nuevos por tipo — `ExpeditionReporterTest.theReportOfAnExpeditionWithANightActivityReflectsItsRisk`; `ExpeditionReporter` no se modificó.
  - Una actividad nocturna dentro de un bloque se valida y se estima igual que suelta — `ItineraryBlocksTest.aNightActivityInsideABlockIsValidatedAndEstimatedLikeALooseOne`.
  - El decorador sobre cada uno de los seis tipos base suma la certificación, la iluminación y el permiso, y conserva la duración y los consumibles — el parametrizado `NightOperationRulesTest.addsTheCertificationTheLightingAndThePermitAndKeepsTheDurationAndTheSupplies`.
  - Los cuatro tipos nuevos y el registro — `DroneSurveyRulesTest`, `WildlifeObservationRulesTest`, `WaterQualityMeasurementRulesTest`, `TransferRulesTest`, `ActivityTypeRegistryTest` (cada tipo por nombre, la variante nocturna, tipo desconocido, parámetro faltante o inválido) y `QueryActivityTypesTest`.
  - Una actividad nocturna se agenda **de punta a punta sobre HTTP** — `ActivityApiTest.schedulesANightActivityAskedForByItsTypeAndAnswersWhatItDemands`, que registra la zona, la persona certificada y la iluminación, crea la expedición con el permiso nocturno de la zona y agenda la actividad por el nombre de su tipo; que quedó en el itinerario lo verifica `theNightActivityStaysInTheItineraryAndCanBeDependedOn`, con una segunda actividad que depende de ella.
  - El alta por tipo resuelve zona, dependencias y reglas, y falla como corresponde — `ScheduleActivityTest` (12 tests: la zona y el tipo del comando, otro tipo con su parámetro, las dependencias del mismo itinerario, la variante nocturna por lo que exige, tipo desconocido, parámetro faltante e inválido, zona, dependencia y expedición inexistentes) y, sobre HTTP, los cuatro errores de `ActivityApiTest` (400 y 404).
  - Los tipos y sus parámetros se publican — `ActivityTypeApiTest.listsTheSixTypesWithTheParameterEachOneNeeds`.

### F2 — Bloques de actividades secuenciales y paralelos

- **Decisiones:** D39, D40, D41, D54, D55, D56.
- **Clases agregadas:**
  - `business.interfaces.itinerary.ItineraryNode` — contrato común de una actividad agendada y un bloque.
  - `business.interfaces.itinerary.ItineraryNodeVisitor<T>` — operación que distingue hoja de bloque desde afuera del dominio, sin `instanceof` (D54).
  - `business.models.expeditions.ItineraryBlock` — clase abstracta con lo que comparten los bloques: hijos, período calculado, riesgo, consumo y hojas.
  - `business.models.expeditions.SequentialBlock` — bloque cuyos hijos corren uno después del otro; su duración es la suma.
  - `business.models.expeditions.ParallelBlock` — bloque cuyas ramas pueden correr a la vez; su duración es el máximo.
  - `business.models.expeditions.PlannedNode` — declaración de lo que se agenda: actividad con horario, bloque o ítem terminado.
  - `business.usecases.ScheduleBlock` y `ScheduleBlockCommand` — agendar un bloque por id de expedición.
  - `business.usecases.PlannedActivityCommand` y `PlannedNodeCommand` — la descripción de una hoja y del árbol que trae el comando de alta de bloque, por ids, tipo y parámetros (D55).
  - `business.usecases.PlannedActivities` — el resolver de zona, dependencias y reglas, compartido entre `ScheduleActivity` y `ScheduleBlock` (D55).
  - `adapters.driving.rest.ItineraryController` (módulo `web`) — `POST /api/expeditions/{id}/blocks` y `GET /api/expeditions/{id}/itinerary`.
  - `adapters.driving.rest.ExpeditionPlanController` (módulo `web`) — `GET /api/expeditions/{id}/validation` y `/estimate`.
  - `adapters.driving.rest.Locations` (módulo `web`) — la dirección que los controllers prometen en `Location` cuando lo que se crea es un nodo del itinerario, sin id propio.
  - `rest.dtos.ScheduleBlockRequest`, `ItineraryNodeResponse`, `ItineraryResponse`, `ValidationFindingDto`, `ValidationSubjectDto`, `ValidationResponse`, `EstimateResponse` y `rest.mappers.ItineraryMapper`, `ValidationMapper`, `EstimateMapper` (módulo `web`) — D54, D55.
  - `adapters.driving.rest.AssignmentController` (módulo `web`) — `POST .../activities/{activityId}/staff`, `/equipment`, `/supplies` y `GET .../candidates`, sobre `AssignResources` y `FindAssignmentCandidates`, que ya existían como casos de uso sin cambios de dominio (D56).
  - `rest.dtos.AssignResourceRequest`, `AssignmentCandidatesResponse`, `StaffCandidatesDto`, `EquipmentCandidatesDto`, `SupplyCandidatesDto` y `rest.mappers.AssignmentCandidatesMapper` (módulo `web`) — D56.
- **Clases modificadas:**
  - `ItineraryNode` — suma `accept(ItineraryNodeVisitor<T>)` (D54).
  - `ItineraryItem` — implementa `ItineraryNode` como hoja y delega en su `Activity`; implementa `accept` llamando a `visitor.activity(this)`.
  - `ItineraryBlock` — suma `acceptedChildren`, package-private, para que `SequentialBlock` y `ParallelBlock` le entreguen al visitor las respuestas de sus hijos ya resueltas.
  - `Itinerary` — guarda nodos raíz; suma `getNodes()`, `runInParallel` y `planKeepingStructure`; el alta recibe un nodo entero y verifica las dependencias de todas sus hojas; `addFinishedCopyOf` se eliminó.
  - `Expedition` — suma `schedule(PlannedNode)`; `addFinishedCopyOf` se eliminó.
  - `ExpeditionBuilder` — suma `addNode(PlannedNode)`; `addActivity` y `addFinishedItem` pasan a ser atajos.
  - `ExpeditionEstimator` — agrega sobre los nodos raíz en vez de sobre las actividades.
  - `ResourceOverlapValidation` — además del horario, considera las ramas paralelas.
  - `ExpeditionReplanner` — arma la alternativa a partir del plan que conserva la estructura.
  - `ScheduleActivityCommand` — pasa a ser `(expeditionId, PlannedActivityCommand)`, la misma descripción que usa cada hoja de un bloque (D55).
  - `ScheduleActivity` y `ScheduleBlock` — los dos reciben `ExpeditionRepository`, `ZoneRepository` y `ActivityTypeRegistry`, y resuelven cada actividad con un `PlannedActivities` por pedido (D55).
  - `ActivityController` — responde con `Location` apuntando al itinerario de la expedición; `ActivityMapper` expone `toPlannedActivity`, compartido con `ItineraryMapper`.
  - `FieldOpsConfig` (módulo `web`) — expone `ScheduleBlock` como bean con sus tres colaboradores nuevos, igual que los demás casos de uso (D37).
  - `ScheduledActivityResponse` — suma `assignedStaffIds`, `assignedEquipmentIds` y `assignedSupplyIds` (D56). `ActivityMapper.toResponse` los completa desde `ItineraryItem`, sin tocar el dominio; sus tres `toDto` de requisitos pasaron de privados a package-private para que `AssignmentCandidatesMapper` los reuse.
- **Refactorizaciones realizadas:**
  - `Itinerary` pasó de una lista de ítems ordenada en cada alta a una lista de nodos raíz en orden de alta, con `getItems()` calculado. Hizo falta para tener el árbol sin cambiar lo que ven los clientes de las hojas.
  - El chequeo de dependencias de `Itinerary` pasó de mirar un ítem contra el itinerario a mirar todas las hojas de un nodo contra el itinerario y contra el propio nodo, para que un bloque entre entero.
  - `ExpeditionBuilder` reemplazó su `record` privado `PlannedActivity` y su lista de terminados por una sola lista de `PlannedNode`, y los dos caminos de alta (`schedule` y `addFinishedCopyOf`) quedaron en uno.
  - `ExpeditionReplanner` dejó de recorrer los ítems dos veces (terminados y pendientes) y de ordenarlos por inicio: le pide el plan al itinerario y solo decide qué pasa con cada actividad.
  - El resolver de zona, dependencias y reglas que D53 dejó privado en `ScheduleActivity` salió a `PlannedActivities`, para que `ScheduleBlock` lo reuse sin duplicarlo (D55).
- **Deuda técnica que se decidió no resolver:**
  - **Un retraso dentro de un secuencial no corre a los hermanos siguientes.** Si la actividad retrasada pasa a pisar a la siguiente, la alternativa falla con `InvalidScheduleException` en vez de reprogramar el resto del bloque. Se aceptó porque correr a los hermanos es una política de reprogramación que el replanificador todavía no tiene (D33 descartó reagendar automáticamente). La Fase 4 lo va a necesitar para ofrecer la reprogramación de F3: ahí hay que definir cuánto se corre cada hermano y qué pasa con las ventanas. **Resuelto en F3 (D46):** se corren por el mismo retraso los hermanos siguientes y los dependientes; si alguno sale de su ventana, la opción no se ofrece.
  - **Dependencias entre bloques.** Las dependencias son entre actividades; no se puede declarar que un bloque va después de otro. Se aceptó porque el enunciado no lo pide y se puede expresar con las dependencias de sus actividades o metiendo ambos en un secuencial. Habría que resolverlo si aparecen bloques reutilizables o dependencias que hoy obligan a repetirse en cada actividad.
  - **Editar un bloque ya agendado.** No se puede agregar un hijo, sacar una actividad ni moverla de un bloque a otro: un bloque se agenda entero y no tiene identificador propio. Se aceptó porque en borrador se puede rearmar el itinerario y, una vez aprobado, el cambio es una replanificación. Habría que resolverlo si la API necesita editar bloques, y ahí el bloque necesita un id.
  - **El árbol no se expone hacia afuera.** `ItineraryNode` no publica los hijos ni el tipo de nodo, y el informe no muestra la estructura. Queda para el endpoint de bloques, que todavía no existe. **Resuelto en el tercer tramo del PR 5b (D54, D55):** `GET /api/expeditions/{id}/itinerary` muestra el árbol con un Visitor, y `POST /api/expeditions/{id}/blocks` lo agenda desde un árbol de descripciones.
  - **`runInParallel` recorre el árbol por cada par de actividades.** Se aceptó por el tamaño de un itinerario; habría que precalcular los pares si la validación se vuelve lenta.
- **Criterios de aceptación:**
  - Puede construirse un itinerario con un bloque paralelo dentro de un bloque secuencial — `ItineraryBlocksTest.anItineraryCanHaveAParallelBlockInsideASequentialBlock` y, por la API, `ScheduleBlockTest.buildsAParallelBlockInsideASequentialOneFromTheTreeOfTheCommand` e `ItineraryApiTest.anItineraryCanHaveAParallelBlockInsideASequentialBlock` sobre HTTP, con `theItineraryIsReadBackWithTheShapeItWasBuiltWith` y `anItineraryWithoutBlocksIsReadAsAListOfActivities` para la lectura.
  - La duración estimada de un bloque secuencial es la suma de sus partes y la de un bloque paralelo, el máximo — `ExpeditionEstimatorTest.theDurationOfASequentialBlockIsTheSumOfItsPartsAndTheOneOfAParallelBlockIsTheMaximum`, el parametrizado `theDurationOfABlockDependsOnWhetherItsPartsRunInSequenceOrInParallel` (secuencial, paralelo, paralelo dentro de secuencial y secuencial dentro de paralelo), y por la API, `ExpeditionPlanApiTest.theEstimateAddsASequentialBlockAndTakesTheMaximumOfAParallelOne` sobre `GET /estimate`.
  - La validación detecta conflictos de recursos entre actividades paralelas del mismo bloque — `ResourceOverlapValidationTest.detectsAResourceConflictBetweenParallelActivitiesOfTheSameBlock`; que se reporta una sola vez e impide aprobar, `ApprovalPolicyTest.aResourceConflictInAParallelBlockIsReportedOnceAndPreventsTheApproval`. **Sobre HTTP** (D56), con el recurso asignado por `AssignmentController`: `ExpeditionPlanApiTest.theValidationReportsAResourceConflictBetweenParallelActivitiesOfTheSameBlock`; `theValidationReportsWhatIsMissingAsACriticalAndSaysItCannotBeApproved` sigue cubriendo un hallazgo que no necesita asignación.
  - Un bloque participa de los informes como cualquier actividad — `ExpeditionReporterTest.theReportOfAnExpeditionWithBlocksUsesTheDurationEachBlockEstimates`.
  - Un itinerario sin bloques valida, estima e informa igual que antes — `ItineraryWithoutBlocksRegressionTest`.
  - Los invariantes de D40 — `ItineraryBlocksTest` (`rejectsAnEmptyBlock`, `rejectsABlockWithAChildOutsideTheExpeditionPeriod`, `rejectsASequentialBlockWhoseChildrenOverlap`, `rejectsADependencyBetweenParallelBranchesEvenIfTheirPeriodsDoNotOverlap` y los demás) y, por el comando de alta, `ScheduleBlockTest` (los mismos casos, más `rejectsAKindOfBlockThatIsNeitherSequentialNorParallel`) e `ItineraryApiTest` (`answersBadRequestForABlockWithNothingInside`, `answersBadRequestForAKindOfBlockThatDoesNotExist`, `answersBadRequestForASequentialBlockWhoseChildrenOverlap`, `answersBadRequestForADependencyBetweenBranchesOfTheSameParallelBlock`).
  - La alternativa de una replanificación conserva los bloques — `ExpeditionReplannerTest.theAlternativeKeepsTheBlocksOfTheOriginal`.
  - Una hoja de un bloque puede depender de una hermana anterior del mismo bloque, y no de una posterior (D55) — `ScheduleBlockTest.aLeafCanDependOnAnEarlierLeafOfTheSameBlock` y `aLeafCannotDependOnALaterLeafOfTheSameBlock`, y por la API, `ItineraryApiTest.anActivityOfABlockCanDependOnAnEarlierOneOfTheSameBlock` y `answersNotFoundWhenAnActivityDependsOnALaterOneOfTheSameBlock`.
  - El árbol se lee con la misma forma con la que se escribió, sin condicionales por tipo de nodo (D54) — `ItineraryBlocksTest.aVisitorReadsTheShapeOfTheTreeWithoutAnyoneAskingWhatKindEachNodeIs`.

### F3 — Replanificación automática tras un incidente

- **Decisiones:** D45, D46, D47, D48.
- **Clases agregadas:**
  - `business.interfaces.events.DomainEvent` y `EventSubscriber` — un evento de dominio y quien reacciona a un tipo de evento.
  - `business.models.events.IncidentRegistered` — el evento que publica el registro de un incidente.
  - `business.providers.EventPublisher` — puerto de salida para publicar eventos, con entrega síncrona.
  - `business.providers.ReplanProposalRepository` — puerto de salida de las propuestas.
  - `business.models.expeditions.IncidentId` e `IncidentReport` — el id que la expedición le da a cada incidente y lo que se informa desde el campo (descripción, fecha, recurso y retraso opcionales).
  - `business.models.expeditions.ResourceAssignment` — un recurso con la forma de asignarlo.
  - `business.models.replanning.ReplanProposal`, `ReplanProposalId`, `ReplanProposalStatus`, `ReplanOption` y `ReplanDecision` — la propuesta, sus opciones, su estado y la decisión.
  - `business.interfaces.replanning.ReplanOptionGenerator` — genera las opciones de un tipo para un incidente.
  - `business.replanning.RescheduleOptions`, `ResourceReplacementOptions` y `CancellationOptions` — los tres generadores; `OptionSummaries` arma sus resúmenes.
  - `business.replanning.ResourceReplaced` — motivo de replanificación: cambia un recurso por otro en el mismo horario.
  - `business.usecases.RegisterIncident` y `RegisterIncidentCommand` — registrar un incidente y obtener la propuesta.
  - `business.usecases.ProposeReplan` — suscriptor que arma y guarda la propuesta.
  - `business.usecases.DecideReplanProposal` y `ReplanDecisionCommand` — aceptar una opción o rechazar.
  - `business.usecases.QueryReplanProposals` — consultar una propuesta o las de una expedición.
  - `business.exceptions.NotAResponsibleException`, `ProposalAlreadyDecidedException` y `DuplicateReplanProposalException`.
  - `adapters.driven.InMemoryEventPublisher` e `InMemoryReplanProposalRepository` (módulo `app`).
  - `ReplanningConfig` (módulo `web`) — los beans de F3 en su propia `@Configuration` (D37).
- **Clases modificadas:**
  - `Incident` — suma id, actividad afectada, recurso y retraso informados.
  - `Expedition` — `registerIncident` reemplaza a `reportIncident` y guarda la bitácora (`getIncidents`); suma `suspendToReplan` y `requireResponsible`; el constructor recibe los incidentes heredados.
  - `ExpeditionBuilder` — suma `addInheritedIncident`.
  - `ActivityTracking` — ya no guarda incidentes.
  - `ItineraryNode`, `ItineraryItem`, `ItineraryBlock` y `SequentialBlock` — suman `itemsAfter`.
  - `Itinerary` — suma `dependentsOf` y `movingWith`.
  - `ReplannedActivity` — suma los recursos que la actividad gana (`additions`).
  - `ExpeditionReplanner` — suma `canPlan`; aplica la cascada de cancelación y el arrastre de un retraso; rehace una actividad afectada que ya empezó; copia la bitácora de incidentes a la alternativa.
  - `ExpeditionReporter` — toma los incidentes de la expedición.
  - `Lookups` — suma la búsqueda de una propuesta.
  - `FieldOpsConfig` (módulo `web`) — el `ExpeditionReplanner` pasa a ser un bean compartido.
  - No se modificaron `ReplanReason`, `Delay`, `Cancellation`, `ResourceWithdrawn`, `ApproveExpedition`, `ReplanExpedition`, `ExpeditionStatus` ni las validaciones.
- **Refactorizaciones realizadas:**
  - `ExpeditionReplanner` pasó de una lista de ítems llevados a un mapa por ítem, para que la cascada pueda sacar o correr ítems ya cargados, y separó el armado de la alternativa (`draftOf`) de la verificación del estado, para reusarlo en `canPlan`.
  - Los incidentes se movieron del seguimiento a la expedición (D48). Hizo falta porque una actividad pendiente no tiene seguimiento.
  - Cambiaron tres tests del replanificador que esperaban el comportamiento anterior: la cancelación con dependientes (ahora cascada), el retraso dentro de un secuencial (ahora corre a los hermanos) y la actividad afectada ya terminada (ahora se rehace). También los tests de seguimiento, del informe y de integración que usaban `reportIncident`.
- **Deuda técnica que se decidió no resolver:**
  - **Entrega sin transacción ni reintentos.** Si el suscriptor falla, el incidente queda registrado sin propuesta. Se aceptó porque todo está en memoria y en el mismo hilo; con persistencia real hace falta una transacción o un outbox (D45).
  - **Un incidente por propuesta.** Dos incidentes sobre la misma actividad generan dos propuestas, y aceptar una deja sin efecto a la otra. Habría que agruparlos si los incidentes encadenados se vuelven frecuentes.
  - **El reemplazo es de a un recurso.** Un incidente nombra a lo sumo un recurso. Si se rompen dos cosas, son dos incidentes.
  - **El retraso lo informa una persona.** El sistema no calcula cuánto correr una actividad; sin retraso informado no hay reprogramación.
  - **La propuesta no se actualiza.** Las opciones se calculan al registrar el incidente. Si el itinerario cambia, aceptar vuelve a verificar y falla si la opción ya no se puede armar, pero no se ofrecen opciones nuevas.
  - **Reemplazar la original a mano no deja sin efecto las propuestas pendientes.** Solo aceptar una propuesta lo hace; una replanificación manual aprobada deja las propuestas pendientes, que fallan al aceptarse porque la original ya no se puede reemplazar.
  - **La API de F3 no existe todavía.** Los casos de uso están expuestos como beans; los endpoints esperan el esqueleto de la Fase 5.
- **Criterios de aceptación:**
  - Registrar un incidente sobre una expedición en ejecución genera una propuesta de replanificación — `IncidentReplanningTest.registeringAnIncidentOnAnExpeditionInExecutionGeneratesAReplanProposal`; con el publicador real, al volver de `RegisterIncident` la propuesta ya existe, `IncidentReplanningIntegrationTest`.
  - El responsable puede aceptarla — `IncidentReplanningTest.theResponsibleCanAcceptAnOptionAndGetsTheAlternativeAsADraft`.
  - El responsable puede rechazarla — `IncidentReplanningTest.theResponsibleCanRejectTheProposalAndTheExpeditionKeepsRunningAsItWas`.
  - El plan original, el incidente y la decisión quedan auditados y consultables — `IncidentReplanningTest.theOriginalPlanTheIncidentAndTheDecisionStayAuditedAndQueryable` y `IncidentReplanningIntegrationTest.anIncidentInTheFieldEndsInAnApprovedAlternativeWithTheWholeHistoryQueryable`.
  - Qué opciones se generan según el incidente y el itinerario — el parametrizado `ProposeReplanTest.offersTheOptionsThatMakeSenseAndCanBeBuilt`, y que toda opción ofrecida se puede armar, `neverOffersAnOptionThatCannotBeAccepted`; cada generador en `ReplanOptionGeneratorsTest`.
  - Reglas de la decisión (solo un responsable, una vez, todo o nada, actividad en curso, propuestas que quedan sin efecto) — `DecideReplanProposalTest`.
  - `RegisterIncident` publica el evento y el suscriptor guarda la propuesta, con Mockito — `RegisterIncidentTest`; el publicador en memoria, `InMemoryEventPublisherTest`.
  - Contrato del repositorio de propuestas — `ReplanProposalRepositoryContract`, que corre `InMemoryReplanProposalRepositoryTest`.
  - La cascada y el arrastre — `ItineraryCascadeTest` y los tests nuevos de `ExpeditionReplannerTest`; la bitácora de incidentes, `ExpeditionIncidentsTest`.

## Patrones que decidimos no aplicar

| Patrón | Por qué no                                                                                                                                                                                              | Consecuencias |
|--------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| Decorator (para las advertencias aceptadas) | Para "advertencia aceptada con justificación" alcanza con `AcceptedWarning`, un `record` que referencia el resultado y le suma responsable y justificación (D22). No hace falta que la advertencia aceptada se comporte como un `ValidationResult`. El patrón sí se usa en F1, donde hay que sumar comportamiento a las reglas de cualquier tipo de actividad (`NightOperationRules`, D42): ahí lo decorado tiene que seguir respondiendo como una `ActivityRules`. | Si más adelante se necesita apilar comportamiento sobre un resultado de validación (ej. distintos tipos de aceptación con reglas propias), habría que reconsiderarlo. |
| State (máquina de estados formal) | Las transiciones válidas son una tabla dentro de `ExpeditionStatus` (D20). Lo que depende del estado son unas pocas preguntas: si el itinerario se puede modificar (D19), si se registra seguimiento (D23), si se puede terminar (D29) y si se puede reemplazar (D33); las dos últimas se responden con la misma tabla de transiciones. Una clase por estado agregaría siete clases para responder esas cuatro preguntas. | Si cada estado empieza a habilitar o prohibir muchas operaciones distintas (ej. qué se puede registrar en ejecución o durante una suspensión), los chequeos de estado se repetirían en `Expedition` y convendría pasar a State. |
| Strategy de conversión entre sistemas de medida | Consideramos una posible extensión a futuro con más de un sistema de medida (ej. métrico e imperial: litros y galones, kilogramos y libras). Hoy hay un único sistema, así que `MeasurementUnit` es un enum simple y `Quantity` rechaza operar entre unidades distintas en vez de convertirlas. | Si se agrega otro sistema, habría que introducir una abstracción de conversión (ej. `UnitConverter`) que `Quantity` use para normalizar antes de sumar, restar o comparar. Como hoy toda la aritmética de unidades está encapsulada en `Quantity` (D8), el cambio queda en ese value object y no se propaga al resto del dominio. |
| Servicio dedicado de asignación (`AsignacionService`) | El enunciado pide "proponer o realizar asignaciones", y ya está cubierto sin una clase intermedia: `ResourceCatalog` (D18) responde quién puede cubrir un requisito (Information Expert sobre el catálogo) y `Expedition.assignStaff`/`assignEquipment`/`assignSupply` (D19) realiza la asignación verificando el estado. Un servicio aparte solo delegaría en las dos sin agregar comportamiento propio. | Si aparece una regla propia de asignación (ej. elegir entre varios candidatos con un criterio de optimización, no solo listarlos), ahí se justifica introducir un servicio que la encapsule. |
| Clean Architecture | La separación que necesitamos es adentro / afuera del dominio, y la hexagonal (D35) la da con menos piezas. Los anillos adicionales (entidades por un lado, casos de uso por otro, con modelos de entrada y salida propios) duplicarían clases sin que haya una regla que lo justifique en este dominio. | Si los casos de uso necesitaran protegerse de cambios de las entidades (por ejemplo, modelos de entrada y salida propios por caso de uso), habría que reconsiderarlo. |
| DDD táctico completo | El dominio es un solo contexto con un agregado claro (`Expedition`) y los bloques que sirven ya están aplicados: entidades, value objects (D8), servicios de dominio y repositorio (D28). Bounded contexts, eventos de dominio y fábricas por agregado sumarían ceremonia sin un segundo contexto que las justifique. Desde F3 se usa un evento de dominio (`IncidentRegistered`, D45) porque el registro de un incidente y la propuesta son responsabilidades distintas, pero sin el resto del aparato: no hay bounded contexts ni eventos acumulados en el agregado. | Si aparece un segundo contexto con lenguaje propio (por ejemplo logística de flota separada de planificación), habría que separar módulos por contexto. |
| Modo del bloque como `enum` (un solo bloque con `SEQUENTIAL` / `PARALLEL`) | Entre un bloque secuencial y uno paralelo cambia comportamiento: cómo se combina la duración, qué se exige a los hijos y si las ramas cuentan como simultáneas. Con un `enum`, cada una de esas preguntas sería un `if` sobre el modo repetido en el bloque, en la validación y en la replanificación. Son dos clases, `SequentialBlock` y `ParallelBlock`, sobre una base común (D39). | Hay una clase por forma de combinar. A cambio, sumar una forma nueva es sumar una clase y no revisar cada condicional. |
| Visitor para las operaciones propias del dominio sobre el árbol del itinerario | Lo que el dominio le pregunta a un nodo son pocas operaciones estables (duración, riesgo, consumo, hojas, si dos actividades corren en paralelo, el plan que conserva su forma), y cada una tiene una respuesta natural por tipo de nodo, así que van como métodos de `ItineraryNode` (D39). Un Visitor separaría cada una en una clase aparte y obligaría a tocar todos los visitors al sumar un tipo de bloque, sin que ninguna de ellas lo necesite. **Revisado en D54:** cuando apareció una operación nueva y de afuera del dominio (mostrar el árbol en la respuesta del itinerario), con un motivo distinto (no exponer la estructura a los clientes del dominio, D39), ahí sí se usó un Visitor (`ItineraryNodeVisitor`). Esta fila sigue describiendo por qué las operaciones del propio dominio no lo usan. | Cada operación nueva del dominio sobre el árbol agrega un método a `ItineraryNode` y a sus implementaciones. Cada operación nueva de afuera (otra vista del árbol) agrega un método a `ItineraryNodeVisitor` y a cada visitor existente (D54). Si las operaciones del dominio empezaran a crecer mucho más rápido que los tipos de nodo, convendría reconsiderar también esa parte. |
| Event Sourcing | La auditoría que pide F3 (plan original, incidente y decisión) ya queda con lo que hay: la expedición original no se reescribe (D25), la alternativa la referencia (D33) y la propuesta guarda el incidente y la decisión (D48). Reconstruir el estado de la expedición desde sus eventos obligaría a modelar cada cambio como evento, versionarlos y guardarlos, para responder lo mismo. | Si hiciera falta reconstruir el estado de una expedición en cualquier momento, o auditar cada asignación y transición y no solo las replanificaciones, habría que reconsiderarlo. |
| Bean Validation en los DTOs de entrada (`@Valid` y anotaciones de `jakarta.validation`) | Las reglas ya están en el dominio: `DomainArguments` y los constructores rechazan un nombre vacío, una lista de objetivos vacía o un período al revés (D8, D49). Repetirlas en los DTOs daría dos fuentes de verdad que se desincronizan, y la validación de la API quedaría más laxa o más estricta que la del negocio según quién toque cuál. El adaptador no valida: deja pasar lo que llegó y traduce la excepción del dominio a 400. | Los mensajes de error son los del dominio, de a uno por pedido, y no una lista de todos los campos mal a la vez. Si hiciera falta responder todos juntos, la salida es que la validación del dominio devuelva varios resultados (como hace `ApprovalPolicy` con las críticas), no anotar los DTOs. |
| Entrega asíncrona de eventos (bus o cola) | El criterio de F3 exige que la propuesta exista cuando vuelve el registro del incidente. Con un solo proceso y todo en memoria, la entrega síncrona del `InMemoryEventPublisher` lo garantiza sin reintentos ni orden de entrega que resolver (D45). | Si un suscriptor se vuelve lento o externo (avisar por correo, otro servicio), habría que sumar un adaptador asíncrono detrás del mismo `EventPublisher`, con un outbox para no perder eventos. |

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
- El enunciado nombra seis estados de una expedición. Agregamos un séptimo, reemplazada (`REPLACED`), para la expedición original cuando se aprueba la alternativa de una replanificación (D33): no es una cancelación ni una finalización, y necesitaba su propio estado terminal.
- En un bloque secuencial se admiten huecos entre un hijo y el siguiente: el enunciado dice que se ejecutan en secuencia, no que sean contiguos (D40).
- Un bloque paralelo declara que sus ramas pueden ejecutarse a la vez. Por eso dos ramas no comparten un recurso reutilizable ni tienen dependencias entre sí, aunque sus horarios agendados no se pisen (D40, D41).
- La duración estimada de un bloque sale de la duración estimada de sus actividades (D4), no de su período agendado: el período dice cuándo ocurre y la estimación, cuánto trabajo hay.
- El consumo estimado de un bloque paralelo es la suma del de sus ramas y no el máximo: los consumibles se gastan aunque las actividades corran a la vez.
- Los valores de `DroneSurveyRules`, `WildlifeObservationRules`, `WaterQualityMeasurementRules` y `TransferRules` (tiempos, umbrales de riesgo, personal, equipamiento y consumo) son ilustrativos, igual que los de los dos tipos de la Entrega 1: el enunciado no los define.
- Una actividad nocturna se declara como tal al crearla; no se infiere de su horario. El enunciado la describe como una variante con requisitos propios, y cuándo es de noche depende del lugar y de la época, que el modelo no conoce. Nada verifica que su horario agendado caiga de noche.
- El permiso especial de operación nocturna es uno por zona y se llama `night operations in zone <id de la zona>`, en minúsculas y con el id tal cual (D43). Lo exige la actividad nocturna y no la zona: una expedición sin actividades nocturnas en esa zona no lo necesita.
- El factor de riesgo nocturno es un nivel más sobre el riesgo del tipo base, con `EXTREME` como máximo (D42): el enunciado pide un factor adicional y no lo cuantifica.
- El equipamiento de iluminación es un equipo de categoría `Lighting`, una unidad por actividad nocturna sin importar cuántas personas participen.
- "Todo el personal asignado" son las personas asignadas a la actividad. La certificación de operación nocturna no se exige a los vehículos ni a los instrumentos, aunque también puedan tener certificaciones (D10).
- Un bloque con un solo hijo es válido, y un bloque no tiene nombre ni identificador propio: el enunciado no pide referirse a un bloque, solo agruparlo y ejecutarlo (D39, D40).
- Un incidente se registra sobre una actividad no terminada, pendiente o en curso, y solo con la expedición en ejecución (D23, D47). Puede nombrar un recurso asignado a esa actividad y un retraso estimado; los dos son opcionales.
- El retraso de una reprogramación es el que informa quien registra el incidente: el enunciado no dice cuánto correr una actividad, y quien está en el campo es quien lo sabe (D46).
- Una propuesta ofrece hasta tres candidatos de reemplazo, en el orden del catálogo: el enunciado no da un criterio para elegir entre ellos, así que decide el responsable (D46).
- Cancelar una actividad cancela las que dependen de ella, y correrla corre lo que viene después en sus secuenciales y lo que depende de ella. Una rama paralela hermana no se corre (D46).
- Aceptar una opción deja la alternativa en borrador; la original queda suspendida hasta que la alternativa se apruebe o se reanude la original (D47).
- La fecha de una decisión la informa quien decide, como la de un incidente o una observación (D47).
