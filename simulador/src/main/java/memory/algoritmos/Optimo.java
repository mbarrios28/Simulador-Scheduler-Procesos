package memory.algoritmos;

import memory.Frame;
import memory.PageTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Optimo implements ReplacementAlgorithm {

	private final Map<String, List<Integer>> futureByProcess = new HashMap<>();

	public void setFutureAccessSequence(String processId, List<Integer> sequence) {
		if (sequence == null) {
			futureByProcess.remove(processId);
		} else {
			futureByProcess.put(processId, new ArrayList<>(sequence));
		}
	}

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
			return Integer.MAX_VALUE;
		}
		for (int i = 0; i < seq.size(); i++) {
			if (seq.get(i) == pageNumber) {
				return i; 
			}
		}
		return Integer.MAX_VALUE; 

	}
	private void consumeOneOccurrence(String processId, int pageNumber) {
		List<Integer> seq = futureByProcess.get(processId);
		if (seq == null || seq.isEmpty())
			return;

		// Buscar y eliminar la PRIMERA ocurrencia de pageNumber
		for (int i = 0; i < seq.size(); i++) {
			if (seq.get(i).equals(pageNumber)) {
				seq.remove(i);
				break;
			}
		}
	}

	@Override
	public Integer chooseVictimFrame(List<Frame> physicalMemory, Map<String, PageTable> processPageTables, String excludeProcessId) {
		if (physicalMemory == null || physicalMemory.isEmpty())
			return null;

		// recuerden chicos que no se requiere víctima
		for (Frame frame : physicalMemory) {
			if (!frame.isOccupied()) {
				return frame.getId();
			}
		}

		Integer victimFrame = null;
		int farthestNextUse = -1; 

		for (Frame frame : physicalMemory) {
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

			if (ownerPid == null || ownerPage == null) {
				return frame.getId();
			}

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
		consumeOneOccurrence(processId, pageNumber);
	}

	@Override
	public void onPageAccess(String processId, int pageNumber) {
		consumeOneOccurrence(processId, pageNumber);
	}

	@Override
	public void onPageUnloaded(String processId, int pageNumber, int frameId) {
	}

	@Override
	public String getName() {
		return "Optimo";
	}
}

