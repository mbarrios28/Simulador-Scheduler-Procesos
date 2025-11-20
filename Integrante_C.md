# Media-Semana 1

## ESTRUCTURA BASE DE MEMORIA

1. Clase Frame:
    - id
    - ocupado/libre
    - proceso y página

2. PageTable:
    - páginas del proceso
    - estados

3. MemoryManager:
    - lista de marcos
    - marcos libres
    - asociaciones proceso → páginas cargadas

4. Funciones iniciales:
    - isPageLoaded()
    - loadPage()

## Objetivos logrados:
    - Memoria representa marcos reales.

## Pruebas obligatorias:
    - 3 marcos, 2 procesos.
    - Imprimir tabla de marcos.


# Media-Semana 2

## ALGORITMOS DE REEMPLAZO

1. FIFO (cola circular).
2. LRU (timestamp o stack).
3. Óptimo (mirar referencia futura).

4. Integrar al manager:
    - elegir víctima
    - reemplazar página

## Objetivos logrados:
    - Reemplazo funcional con los tres algoritmos.

## Pruebas obligatorias:
```java
1 2 3 4 1 2 5...
```


# Media-Semana 3

## INTEGRACIÓN CON SCHEDULER

1. Implementar ensurePages():
    - cargar todas las necesarias
    - detectar fallos
    - bloquear proceso

2. Notificar al scheduler cuando páginas estén listas.

3. Contadores:
    - fallos
    - reemplazos

## Objetivos logrados:
    - Interacción realista entre CPU y memoria.

## Pruebas obligatorias:
    - P1 falla → P2 ejecuta.
    - Retorno correcto al READY.


# Media-Semana 4

## REPORTES DE MEMORIA + INFORME

1. Estado completo de memoria:
    - marcos libres
    - marcos ocupados
    - páginas activas

2. Estadísticas:
    - fallos de página por proceso
    - reemplazos totales

3. Integración con informe.

## Informe Final (Aporta Integrante C – Memoria Virtual)
    - Explicación de memoria física simulada.
    - Tabla de marcos y estructura interna.
    - Descripción detallada de FIFO, LRU y Óptimo.
    - Análisis del impacto en rendimiento.

## Objetivos logrados:
    - Memoria completamente reportada.

## Pruebas obligatorias:
    - Caso final con fallos registrados y correctos.
