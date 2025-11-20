# Media-Semana 1

## HILOS BÁSICOS (ProcessThread)

1. Crear ProcessThread (extends Thread).
2. Implementar sincronización interna:
    - wait() para detener
    - notify() para permitir ejecutar

3. Ejecutar 1 unidad por permiso:
    - simular con sleep()

4. Notificar scheduler al completar unidad.

## Objetivos logrados:
    - Hilos controlados externamente.

## Pruebas obligatorias:
    - 2 procesos → nunca ejecutan simultáneamente.


# Media-Semana 2

## MANEJO DE E/S (IOManager)

1. IOManager:
    - temporizador de E/S
    - no bloquear scheduler

2. Estados:
    - RUNNING → BLOCKED_IO
    - BLOCKED_IO → READY

3. Integrar con ProcessThread.

## Objetivos logrados:
    - E/S no congela la simulación.

## Pruebas obligatorias:
    - P1 en E/S → P2 ejecuta.


# Media-Semana 3

## SINCRONIZACIÓN GENERAL

1. Crear SyncManager:
    - mutex global
    - locks por proceso
    - condiciones de despertar

2. Evitar carreras entre:
    - Scheduler
    - Hilos
    - Memoria
    - E/S

3. Manejar bloqueos simultáneos.

## Objetivos logrados:
    - Simulación estable bajo concurrencia.

## Pruebas obligatorias:
    - Stress test 10 procesos.


# Media-Semana 4

## FINALIZACIÓN + INFORME

1. Depuración final:
    - deadlocks
    - carreras
    - orden de notificaciones

2. Integración final con módulos A, B y C.

## Informe Final (Aporta Integrante D – Hilos y E/S)
    - Explicación de hilos y sincronización.
    - Modelo de ejecución controlada por Scheduler.
    - Manejo de bloqueos de E/S.
    - Garantías de exclusión mutua.

## Objetivos logrados:
    - Hilos sincronizados completamente.

## Pruebas obligatorias:
    - Caso final: P1 CPU/E-S/CPU + P2 CPU + ... .
