# Trabajo Integrador - Entrega 2
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

La aplicación debe impedir que una expedición pase a estado **aprobada** si posee errores críticos. Las advertencias no críticas pueden ser aceptadas por un responsable, dejando registrada la justificación.

## Funcionalidades nuevas de esta entrega

Además de la implementación completa de los requisitos funcionales anteriores y de los requisitos no funcionales, esta entrega incorpora las funcionalidades descritas a continuación.

### F1 - Actividades nocturnas

La organización incorpora operaciones nocturnas. Una actividad nocturna es una variante con requisitos propios: certificación de operación nocturna para todo el personal asignado, permisos especiales por zona, equipamiento de iluminación y un factor de riesgo adicional.

**Criterios de aceptación:** puede crearse una expedición con al menos una actividad nocturna; la validación exige la certificación y el permiso correspondientes; la estimación de riesgo refleja el factor nocturno; los informes existentes incluyen la nueva variante sin condicionales nuevos distribuidos por tipo.

### F2 - Bloques de actividades secuenciales y paralelos

El itinerario debe admitir bloques: agrupaciones de actividades (o de otros bloques) que se ejecutan en secuencia o en paralelo. Un bloque participa de la validación, la estimación y los informes como cualquier actividad.

**Criterios de aceptación:** puede construirse un itinerario con un bloque paralelo dentro de un bloque secuencial; la duración estimada de un bloque secuencial es la suma de sus partes y la de un bloque paralelo, el máximo; la validación detecta conflictos de recursos entre actividades paralelas del mismo bloque.

### F3 - Replanificación automática tras un incidente

Al registrarse un incidente que afecta una actividad, el sistema debe proponer automáticamente un plan alternativo (reprogramación, reemplazo de recursos o cancelación parcial) conservando el historial: el plan original debe seguir siendo consultable, junto con el incidente que motivó el cambio y la decisión tomada.

**Criterios de aceptación:** registrar un incidente sobre una expedición en ejecución genera una propuesta de replanificación; el responsable puede aceptarla o rechazarla; el plan original, el incidente y la decisión quedan auditados y consultables.

## Requisitos no funcionales

### Arquitectura

La arquitectura del proyecto debe seguir una de las vistas en clase: Clean Architecture, Domain Driven Design o Hexagonal.

El backend debe exponer una API REST. El frontend debe contener lo mínimo indispensable para llevar a cabo las funcionalidades pedidas.

### Testing e integración continua

Deben existir pruebas unitarias, de repositorio y de API. Los mocks sustituyen colaboradores en pruebas unitarias; no reemplazan las pruebas reales de persistencia ni de API.

La integración continua debe compilar, ejecutar las pruebas y bloquear la integración de pull requests que fallen.

## Entrega 2 - Aplicación completa

**Objetivo:** comprobar que la aplicación resuelve los principales casos de uso, que su arquitectura respeta límites claros y que el diseño tolera una evolución significativa: las funcionalidades nuevas descritas en este documento.

El entregable central es la aplicación completa: un proyecto ejecutable que implementa todos los requisitos funcionales, incluidas las funcionalidades nuevas, sobre el módulo de dominio, y que cumple los requisitos no funcionales descritos en este documento.

## Entregables obligatorios

| Entregable | Contenido mínimo |
|---|---|
| Repositorio | URL del repositorio de GitHub y hash del último commit que corresponde a la entrega |
| Código de la aplicación | Módulo de dominio, casos de uso, infraestructura (persistencia, integraciones, procesos) y API REST, organizados según la arquitectura elegida |
| Funcionalidades nuevas | F1, F2 y F3 implementadas, probadas y documentadas |
| Tests | Unitarios, parametrizados, de repositorio y de API; deben ejecutarse en la integración continua |
| Datos de demostración | Script, migración o fixture repetible que permita reproducir la demostración sugerida |
| Documentación | README ejecutable con instrucciones para compilar, ejecutar y probar, y contratos de la API |
| Decisiones de diseño | Archivo `../DESIGN.md` en la raíz del repositorio que enumera las decisiones de diseño principales: qué patrón o principio se aplicó, dónde y por qué; qué alternativas se descartaron; y qué patrones se decidió no aplicar, con sus consecuencias. Por cada funcionalidad nueva incluye además las clases agregadas y modificadas, las refactorizaciones realizadas y la deuda técnica que se decidió no resolver |

## Demostración sugerida

La demostración puede incluir una expedición con al menos diez actividades, seis tipos diferentes (incluyendo la actividad nocturna), un bloque compuesto, recursos compartidos, un error crítico, una advertencia aceptable y una replanificación originada en un incidente.

## Qué no se exige en esta entrega

No se exigen microservicios ni despliegue en la nube. El esfuerzo debe concentrarse en el backend, que es lo que se evalúa aquí.
