# Trabajo Integrador - Entrega 3

## Plataforma de expediciones científicas y operaciones de campo

## Introducción

El trabajo integrador se desarrolla en grupos de cinco integrantes sobre esta consigna. La complejidad de negocio es suficiente para que la calidad del modelado, la separación de responsabilidades y la extensibilidad resulten determinantes: no alcanza con que el sistema funcione.

## Enunciado

Una organización dedicada a proyectos científicos y ambientales realiza expediciones en distintas regiones. Cada expedición incluye objetivos, zonas de trabajo, participantes, vehículos, instrumental, permisos, restricciones y un conjunto de actividades que deben ejecutarse dentro de determinadas ventanas temporales.

Actualmente la planificación se realiza con documentos y planillas independientes. Esto provoca asignaciones incompatibles, recursos duplicados, actividades sin personal habilitado y dificultades para modificar el plan cuando ocurre un incidente.

El equipo deberá desarrollar una aplicación que permita planificar, validar, aprobar y seguir la ejecución de expediciones. El sistema deberá soportar diferentes tipos de actividades, cada una con sus propias reglas de duración, recursos, riesgos, dependencias y requisitos de personal.

## Requisitos funcionales obligatorios

| Capacidad | Comportamiento esperado |
|---|---|
| Administración de expediciones | Crear una expedición indicando objetivos, período, zonas, responsables y restricciones generales |
| Catálogo de recursos | Administrar personas, certificaciones, vehículos, instrumentos, consumibles y disponibilidad |
| Construcción del itinerario | Incorporar actividades, ordenarlas, definir dependencias y establecer ventanas temporales |
| Validación | Detectar superposiciones, falta de recursos, certificaciones ausentes, exceso de capacidad y permisos faltantes |
| Estimación | Calcular duración, nivel de riesgo y consumo estimado de recursos |
| Asignación | Proponer o realizar asignaciones de participantes y equipamiento |
| Aprobación | Gestionar estados de borrador, revisión, aprobada, en ejecución, suspendida y finalizada |
| Seguimiento | Registrar comienzo, finalización, observaciones, incidentes y resultados de actividades |
| Replanificación | Construir una alternativa ante cancelaciones, indisponibilidad o retrasos |
| Informes | Generar resumen operativo, consumo de recursos, riesgos e incidentes |

La aplicación debe impedir que una expedición pase a estado aprobada si posee errores críticos. Las advertencias no críticas pueden ser aceptadas por un responsable, dejando registrada la justificación.

## Propuesta de valor: Arquitectura orientada a eventos

La propuesta de valor es un requerimiento adicional que se implementa sobre la aplicación existente y se integra a su arquitectura. Es propia de esta consigna.

La propuesta de valor de esta consigna es incorporar una arquitectura orientada a eventos en los puntos del dominio donde aporta valor real. En un sistema de operaciones de campo, hechos como una aprobación, un incidente o la finalización de una actividad interesan a varias partes del sistema a la vez; los eventos permiten reaccionar a esos hechos sin acoplar al emisor con cada interesado.

| Elemento | Requisito |
|---|---|
| Consumidor asíncrono | Al menos un consumidor procesa eventos fuera del flujo de la petición: notificaciones, generación de la propuesta de replanificación o actualización de informes |
| Registro de eventos | Los eventos emitidos quedan registrados y consultables, integrados a la auditoría existente |

## Requisitos no funcionales

### Arquitectura

La aplicación conserva la arquitectura elegida (Clean Architecture, Domain Driven Design o Hexagonal). La propuesta de valor se integra detrás de contratos propios: el módulo de dominio sigue libre de dependencias de framework y de proveedores externos, y la lógica de negocio no se duplica en la capa nueva.

### Requisitos de la propuesta de valor

| Elemento | Requisito |
|---|---|
| Eventos de dominio | Identificar y modelar al menos tres eventos significativos, por ejemplo `ExpeditionApproved`, `IncidentReported` y `ActivityCompleted`, como objetos inmutables del dominio |
| Publicación desacoplada | El código que produce el hecho no conoce a sus consumidores; puede resolverse con eventos en proceso o con un broker de mensajería |
| Pruebas | Verifican la emisión ante la operación correspondiente y el efecto del consumidor, incluyendo qué ocurre si el consumidor falla |

## Entrega 3 - Propuesta de valor y presentación final

**Objetivo:** incorporar al sistema la propuesta de valor descrita en este documento - o justificar técnicamente por qué no aplica al dominio - y presentarla oralmente, junto con el dominio y las decisiones clave de implementación.

El entregable central es la aplicación completa con la propuesta de valor integrada: el requerimiento adicional se implementa sobre la aplicación existente, respeta su arquitectura y se presenta como se haría ante un cliente o un comité técnico: qué problema resuelve, qué costo tuvo y qué valor agrega.

### Entregables obligatorios

| Entregable | Contenido mínimo |
|---|---|
| Repositorio | URL del repositorio de GitHub y hash del último commit que corresponde a la entrega |
| Código de la aplicación | Aplicación completa con la propuesta de valor integrada, cumpliendo los requisitos funcionales y no funcionales de este documento |
| Tests | Los indicados en los requisitos no funcionales, ejecutables en la integración continua |
| Justificación escrita | Solo si el equipo sostiene que la propuesta no aplica al dominio, según se describe más abajo |
| Presentación oral | Frente a toda la clase, con el formato descrito más abajo; incluye demostración en vivo |
| Decisiones de diseño | Archivo `DESIGN.md` en la raíz del repositorio que enumera las decisiones de diseño principales: qué patrón o principio se aplicó, dónde y por qué; qué alternativas se descartaron; y qué patrones se decidió no aplicar, con sus consecuencias |

### Si la propuesta no aplica al dominio

Si el equipo considera que la propuesta no aplica a su dominio, debe explicar por qué: entregar una justificación técnica escrita y defenderla en la presentación, indicando qué problemas o costos tendría incorporarla y qué alternativa aportaría un valor equivalente. Una justificación sólida y honesta vale tanto como una implementación; una excusa para evitar trabajo, no.

### Presentación oral

La presentación es oral y frente a toda la clase. Presenta un integrante por grupo, elegido aleatoriamente al comienzo de la clase de presentaciones. Por lo tanto, todos los integrantes deben estar preparados para presentar el trabajo completo. El resto del grupo puede participar durante la ronda de preguntas. La gestión del tiempo disponible es parte de la evaluación.

| Momento | Contenido |
|---|---|
| Dominio | Problema, actores, reglas centrales y puntos de extensión del diseño. La audiencia no conoce este dominio, así que debe poder entenderlo desde cero |
| Propuesta de valor | Qué es, por qué aporta valor en este dominio, cómo se integró a la arquitectura y demostración. Si el grupo argumentó que no aplica, presentación de esa justificación |
| Decisiones clave | Las decisiones de implementación más importantes: alternativas consideradas, decisión tomada y consecuencias observadas |
| Preguntas | Preguntas del docente y de la clase; pueden dirigirse a cualquier integrante del grupo |

En la presentación, el equipo debe mostrar el flujo completo de un evento (quién lo emite, quién lo consume y qué garantías tiene la entrega), justificar en qué puntos eligió comunicación por eventos y en cuáles mantuvo llamadas sincrónicas, y discutir las consecuencias: consistencia eventual, orden, reintentos y duplicados.

### Qué no se exige en esta entrega

No se exige rehacer la aplicación ni incorporar funcionalidades ajenas a la propuesta de valor. El esfuerzo debe concentrarse en integrarla bien a la arquitectura existente y en poder explicar cada decisión.