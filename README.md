# Simulador-Scheduler-Procesos
Proyecto final del curso de Sistemas Operativos

Este proyecto consiste en la implementación completa de un **simulador de Sistema Operativo**.
El simulador integra **cuatro grandes módulos**: Modelo de Procesos, Scheduler, Memoria Virtual y Ejecución mediante Hilos con manejo de Entrada/Salida (E/S).  
Cada módulo está desarrollado por un integrante diferente, pero todos se combinan para formar un único sistema completamente funcional.

El objetivo final es replicar, de manera controlada y didáctica, el comportamiento de un sistema operativo real:  
- planificación de procesos  
- administración de memoria  
- ejecución concurrente mediante hilos  
- bloqueo/desbloqueo por memoria y E/S  
- métricas de rendimiento  
- generación de un diagrama de Gantt  
- producción de un informe técnico estilo IEEE  

---

# Funcionalidades Principales del Simulador

## Representación de procesos
El simulador utiliza un modelo detallado de procesos que incluye:
- PID  
- tiempo de llegada  
- ráfagas CPU/E/S  
- prioridad  
- páginas requeridas  
- estado del proceso  
- índice de ráfaga  
- tiempos de ejecución, espera y retorno  

Los procesos se construyen automáticamente desde el archivo `procesos.txt`.

---

## Planificación y algoritmos
El Scheduler implementa diferentes políticas de planificación:
- **FCFS (First Come, First Served)**
- **SJF (Shortest Job First)**
- **Round Robin** con quantum configurable  

El scheduler controla el tiempo global, administra la cola READY, elige el proceso RUNNING y genera el **diagrama de Gantt**.

---

## Memoria virtual y reemplazo de páginas
Se implementan tres algoritmos clásicos de reemplazo:
- **FIFO**
- **LRU**
- **OPT (Óptimo)**  

El gestor de memoria detecta fallos de página, carga páginas en marcos libres o reemplaza páginas según el algoritmo seleccionado.  
Si un proceso no tiene las páginas necesarias → pasa a **BLOCKED_MEM** hasta que la memoria libere recursos.

---

## Hilos y ejecución real
Cada proceso es representado por un **hilo (Thread real de Java)**.  
Los hilos **no toman decisiones por sí mismos**:  
solo ejecutan cuando el Scheduler se los permite.  

La simulación controla:
- ejecución por unidad de tiempo  
- bloqueos por E/S  
- bloqueos por fallos de página  
- reanudación correcta del proceso  

El IOManager simula tiempos reales de E/S sin bloquear la simulación.

---

# Arquitectura general

El proyecto se divide en 4 módulos que se integran de manera coherente:

| Integrante | Módulo | Responsabilidad |
|-----------|--------|------------------|
| **A** | Modelo del Proceso + Parser | Construcción de Process, ráfagas, parser del archivo |
| **B** | Scheduler | Algoritmos FCFS/SJF/RR, métricas y Gantt |
| **C** | Memoria Virtual | Reemplazo FIFO/LRU/OPT, fallos de página |
| **D** | Hilos + E/S | ProcessThread, sincronización, IOManager |

---

# Flujo completo de la simulación

1. **Parser** lee procesos.txt → crea procesos.  
2. **Scheduler** recibe los procesos en NEW y los mueve a READY.  
3. Cuando un proceso está en RUNNING:  
   - el **ProcessThread** ejecuta 1 unidad de CPU.  
4. Antes de ejecutar, el scheduler llama a:  
```java
memory.ensurePages(process)
```
- si falta página → el proceso pasa a BLOCKED_MEM  
5. Si la ráfaga actual es E/S → pasa a BLOCKED_IO.  
6. Según la política de CPU, el scheduler elige el siguiente proceso.  
7. Al final, se registran métricas y se genera el Gantt.  

---

# Informe Final

El informe se divide por módulos con autores asignados:

- **Integrante A:** Modelo del proceso, parser, estructura de ráfagas, estados.  
- **Integrante B:** Planificación CPU, algoritmos, métricas, Gantt.  
- **Integrante C:** Memoria virtual, reemplazo FIFO/LRU/OPT, fallos de página.  
- **Integrante D:** Hilos, sincronización, E/S, ejecución concurrente.

El informe explica:
- diseño del simulador  
- funcionamiento interno  
- interacción entre módulos  
- casos de prueba  
- análisis del rendimiento  

---

# Pruebas principales realizadas

- Validación del parser con múltiples archivos.  
- Ejecución de 2–5 procesos con diferentes ráfagas.  
- Comparación entre FCFS, SJF y RR.  
- Fallos de página con secuencias complejas.  
- Bloqueos simultáneos de E/S y memoria.  
- Gantt validado por unidad de tiempo.  

---

# Requisitos técnicos

- Java 17 o superior  
- Uso de Threads, synchronized, wait/notify  
- No se utiliza bloqueo real de CPU (solo simulación)  
- Arquitectura modular para facilitar pruebas individuales  
