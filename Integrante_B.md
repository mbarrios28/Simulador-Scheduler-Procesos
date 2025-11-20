# Media-Semana 1

## SCHEDULER BASE

1. Crear clase Scheduler.
2. Manejar tiempo global.
3. Implementar cola READY (FIFO simple).
4. Métodos base:
    - addProcess()
    - dispatch()
    - runOneUnit()

5. Estados manejados:
    - NEW → READY
    - READY → RUNNING
    - RUNNING → READY

## Objetivos logrados:
    - Scheduler básico y estable.
    - Integrado con ProcessThread.

## Pruebas obligatorias:
    - 1 proceso corriendo.
    - 2 procesos alternando sin errores.


# Media-Semana 2

## ALGORITMOS DE PLANIFICACIÓN (FCFS, SJF, RR)

1. Implementar FCFS.
2. Implementar SJF:
    - elegir ráfaga más corta.

3. Implementar RR:
    - manejar quantum
    - reinserción en cola

4. Crear interfaz o plantilla común.

## Objetivos logrados:
    - Scheduler modular y extensible.
    - Los tres algoritmos funcionan correctamente.

## Pruebas obligatorias:
    - FCFS con 3 procesos.
    - SJF con ráfagas distintas.
    - RR con quantum 2.


# Media-Semana 3

## INTEGRACIÓN CON MEMORIA

1. Antes de ejecutar:
```java
memory.ensurePages(process)
```
    
2. Si falta página:
    - RUNNING → BLOCKED_MEM

3. Cuando memoria libera páginas:
    - BLOCKED_MEM → READY

## Objetivos logrados:
    - Scheduler reacciona a fallos de página.
    - No se bloquea la simulación.

## Pruebas obligatorias:
    - P1 bloqueado por memoria → P2 ejecuta.
    - Retorno a READY correctamente.


# Media-Semana 4

## MÉTRICAS + GANTT + INFORME

1. Calcular métricas:
    - espera
    - retorno
    - respuesta
    - utilización de CPU

2. Generar Gantt:
    - registrar ejecución por unidad de tiempo

3. Confirmar integración de datos para informe final.

## Informe Final (Aporta Integrante B – Planificación)
    - Sección completa de algoritmos de planificación.
    - Explicación de FCFS, SJF, RR.
    - Justificación de la política elegida para pruebas.
    - Análisis del gráfico de Gantt.
    - Métricas y rendimiento del sistema.

## Objetivos logrados:
    - Scheduler completo y documentado.

## Pruebas obligatorias:
    - Caso final con Gantt validado.
