package tme1shadow;

import tme1.Message;

public class FIFOBroadcastMessage extends Message {

	private  final long numseq;
	
	public FIFOBroadcastMessage(long idsrc, long iddest, long numseq, String tag, Object content, int pid) {
		super(idsrc, iddest, tag, content, pid);
		this.numseq=numseq;
		
	}
	
	public long getNumseq() {
		return numseq;
	}

}
