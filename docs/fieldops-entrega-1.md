# Trabajo Integrador - Entrega 1
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

## Entrega 1 - Módulo de dominio

**Objetivo:** demostrar mediante código que el equipo comprendió el dominio y estableció un diseño extensible antes de construir la aplicación completa.

El entregable central es el módulo de dominio: un proyecto Java compilable y probado que contiene los modelos del negocio, los contratos y los casos de uso más relevantes para conocer el dominio (implementaciones concretas).

Deben existir tests con los casos más relevantes del negocio únicamente.

## Entregables obligatorios

| Entregable | Contenido mínimo |
|---|---|
| Repositorio | URL del repositorio de GitHub y hash del último commit que corresponde a la entrega |
| Código del dominio | modelos, servicios, casos de uso, interfaces |
| Tests | Unitarios y de Integración, con cobertura significativa de los casos de uso |
| Decisiones de diseño | Archivo `../DESIGN.md` en la raíz del repositorio que enumera las decisiones de diseño principales: qué patrón o principio se aplicó, dónde y por qué; qué alternativas se descartaron; y qué patrones se decidió no aplicar, con sus consecuencias |

## Qué no se exige en esta entrega

No se exige API REST, persistencia real, frontend, seguridad ni despliegue. Únicamente módulo del dominio.
