package memory.algoritmos;

import memory.Frame;
import memory.PageTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Algoritmo Óptimo: expulsa la página cuyo próximo uso
 * ocurrirá más lejos en el futuro (o que no se volverá a usar).
 *
 * Nota: Para ser verdaderamente "óptimo" se requiere conocer la
 * secuencia futura de accesos. Este algoritmo permite (opcionalmente)
 * configurar dicha secuencia por proceso mediante
 * {@link #setFutureAccessSequence}.
 * Si no se configura, hará un fallback razonable: asumirá que ninguna
 * página se volverá a usar y elegirá el primer frame ocupado.
 */
public class Optimo implements ReplacementAlgorithm {

	// Secuencia futura de accesos por proceso (lista de números de página).
	// En cada acceso/carga se consume una ocurrencia del número de página.
	// Mapa donde la clave es el ID del proceso y la lista son las paginas futuras.
	private final Map<String, List<Integer>> futureByProcess = new HashMap<>();

	/**
	 * Configura o reemplaza la secuencia completa de accesos futuros para un
	 * proceso.
	 * La lista debe contener, en orden, los números de página que ese proceso
	 * accederá a partir de ahora.
	 */
	public void setFutureAccessSequence(String processId, List<Integer> sequence) {
		if (sequence == null) {
			futureByProcess.remove(processId);
		} else {
			futureByProcess.put(processId, new ArrayList<>(sequence));
		}
	}

	/**
	 * Configura múltiples secuencias futuras de una vez.
	 */
	public void setFutureAccessSequences(Map<String, List<Integer>> sequences) {
		futureByProcess.clear();
		if (sequences != null) {
			for (Map.Entry<String, List<Integer>> e : sequences.entrySet()) {
				setFutureAccessSequence(e.getKey(), e.getValue());
			}
		}
	}

	private int nextUseDistance(String processId, int pageNumber) {
		List<Integer> seq = futureByProcess.get(processId);
		if (seq == null || seq.isEmpty()) {
			// No hay información futura: considerar que no se usará (infinito)
			return Integer.MAX_VALUE;
		}
		// Buscar la siguiente ocurrencia en la secuencia futura.
		for (int i = 0; i < seq.size(); i++) {
			if (seq.get(i) == pageNumber) {
				return i; // distancia hasta el próximo uso
			}
		}
		return Integer.MAX_VALUE; // no vuelve a usarse
	}

	/**
	 * Consume (elimina) una ocurrencia de la secuencia futura cuando se accede a una página.
	 * Solo elimina si el acceso coincide con el primer elemento de la secuencia,
	 * manteniendo la sincronización entre accesos reales y predicción futura.
	 * 
	 * @param processId ID del proceso
	 * @param pageNumber Número de página accedida
	 */
	private void consumeOneOccurrence(String processId, int pageNumber) {
		List<Integer> seq = futureByProcess.get(processId);
		if (seq == null || seq.isEmpty())
			return;

		// Solo consumir si coincide con el primer elemento (secuencia sincronizada)
		if (seq.get(0).equals(pageNumber)) {
			seq.remove(0);
		}
		// Si no coincide, la secuencia está desincronizada o hay un error
		// No consumir para evitar corrupción de la predicción futura
	}

	@Override
	public Integer chooseVictimFrame(List<Frame> physicalMemory, Map<String, PageTable> processPageTables, String excludeProcessId) {
		if (physicalMemory == null || physicalMemory.isEmpty())
			return null;

		// Si hay un frame libre, usarlo directamente (no se requiere víctima)
		for (Frame frame : physicalMemory) {
			if (!frame.isOccupied()) {
				return frame.getId();
			}
		}

		// Memoria llena: elegir al que su próximo uso sea el más lejano
		Integer victimFrame = null;
		int farthestNextUse = -1; // buscamos maximizar esta distancia

		for (Frame frame : physicalMemory) {
			// Identificar qué proceso/página está en este frame
			String ownerPid = null;
			Integer ownerPage = null;
			for (Map.Entry<String, PageTable> entry : processPageTables.entrySet()) {
				Integer pageInFrame = entry.getValue().findPageInFrame(frame.getId());
				if (pageInFrame != null) {
					ownerPid = entry.getKey();
					ownerPage = pageInFrame;
					break;
				}
			}

			// Si no podemos determinar dueño, elegir este por seguridad
			if (ownerPid == null || ownerPage == null) {
				return frame.getId();
			}

			// Saltar si pertenece al proceso excluido
			if (excludeProcessId != null && ownerPid.equals(excludeProcessId)) {
				continue;
			}

			int dist = nextUseDistance(ownerPid, ownerPage);
			if (victimFrame == null || dist > farthestNextUse) {
				victimFrame = frame.getId();
				farthestNextUse = dist;
			}
		}

		return victimFrame;
	}

	@Override
	public void onPageLoaded(String processId, int pageNumber, int frameId) {
		// Al cargarse, esa referencia actual ya se consumió de la secuencia futura.
		consumeOneOccurrence(processId, pageNumber);
	}

	@Override
	public void onPageAccess(String processId, int pageNumber) {
		// Un acceso también consume una ocurrencia futura de esa página.
		consumeOneOccurrence(processId, pageNumber);
	}

	@Override
	public void onPageUnloaded(String processId, int pageNumber, int frameId) {
		// Nada que hacer para óptimo al descargar.
	}

	@Override
	public String getName() {
		return "Optimo";
	}
}

