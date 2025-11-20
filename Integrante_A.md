# Media-Semana 1

## MODELO DEL PROCESO + PARSER

1. Crear clase Burst (CPU/E/S).

2. Crear clase Process con:
    - PID
    - llegada
    - lista de ráfagas
    - prioridad
    - páginas requeridas
    - estado
    - índice de ráfaga actual
    - tiempos de espera, retorno, ejecución

3. Crear clase ProcessState (enum).

4. Crear el archivo InputParser:
    - Leer archivo procesos.txt
    - Reconstruir ráfagas
    - Manejar errores de formato

5. Validar datos del proceso (páginas > 0, ráfagas bien formadas, etc.)

## Objetivos logrados:
    - Todos los procesos se crean correctamente.
    - procesos.txt se transforma en una lista de objetos Process.
    - Estados básicos funcionando (NEW, READY).

## Pruebas obligatorias:
    - Leer un archivo simple.
    - Crear 3 procesos y verificar valores.
    - Validar transiciones de estado manuales.


# Media-Semana 2

## PARSER + ESTADOS AVANZADOS

1. Agregar estados adicionales:
    - BLOCKED_MEM
    - BLOCKED_IO
    - TERMINATED

2. Añadir funciones al Process:
    - nextBurst()
    - isCurrentBurstCPU()
    - isCurrentBurstIO()
    - cambio de estado según ráfaga

3. Completar InputParser:
    - Validar alternancia CPU/E/S
    - Manejar prioridades faltantes
    - Detectar líneas inválidas

4. Integración completa con Burst y Process:
    - Confirmar que todas las ráfagas quedan estructuradas.

## Objetivos logrados:
    - Procesos completamente parseados y estructurados.
    - Estados avanzados implementados correctamente.
    - Ráfagas alternadas CPU/E/S funcionando.

## Pruebas obligatorias:
    - Probar parseo con archivos complejos.
    - Archivo con errores → debe fallar limpiamente.
    - Transiciones de estado según ráfagas.


# Media-Semana 3

## INTEGRACIÓN DEL MODELO CON EL SCHEDULER

1. Enlazar Process con Scheduler:
    - scheduler obtiene ráfaga actual
    - scheduler lee tiempo restante

2. Preparar datos para métricas:
    - llegada
    - ráfagas completadas
    - tiempo de ejecución
    - tiempo de retorno

3. Confirmar compatibilidad con ProcessThread.

## Objetivos logrados:
    - Scheduler puede usar Process sin errores.
    - Métricas compatibles para el informe.
    - Integración estable con los demás módulos.

## Pruebas obligatorias:
    - Probar con 2 procesos simples.
    - Verificar cómputo de ráfagas.
    - Confirmar estados y cambios de ráfaga.


# Media-Semana 4

## FINALIZACIÓN DEL MÓDULO Y DOCUMENTACIÓN

1. Implementar funciones de logging dentro de Process.
2. Resumen final del proceso:
    - tiempos
    - ráfagas completas
    - bloqueos por E/S o memoria

3. Comprobar integración final con Scheduler, Memoria y Hilos.

## Informe Final (Aporta Integrante A – Modelo y Parser)
    - Descripción del modelo del proceso.
    - Justificación de diseño (Burst, ráfagas alternadas, estados).
    - Sección de formato del archivo procesos.txt.
    - Diagrama del ciclo de vida del proceso.
    - Explicación de transición de estados.

## Objetivos logrados:
    - Estructura final del proceso implementada.
    - Compatibilidad total con el simulador.

## Pruebas obligatorias:
    - Reporte de 4 procesos completos.
    - Validación de estados finales.
