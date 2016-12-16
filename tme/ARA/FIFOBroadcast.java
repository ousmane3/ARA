package tme1shadow;

import java.util.ArrayList;
import java.util.List;

import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import tme1.BroadcastProtocol;
import tme1.Message;

public class FIFOBroadcast implements BroadcastProtocol, EDProtocol {

	
	private static final String PAR_BROADCASTID = "broadcast";
	
	private final int protocol_id;
	private final int broadcast_protocol_id;
	
	private int[] next;
	private List<FIFOBroadcastMessage> pendMsg;
	
	private int cur_num_seq=0;
	
	public FIFOBroadcast(String prefix) {
		String tmp[]=prefix.split("\\.");
		protocol_id=Configuration.lookupPid(tmp[tmp.length-1]);
		broadcast_protocol_id=Configuration.getPid(prefix+"."+PAR_BROADCASTID);
		
		next = new int[Network.size()];
		pendMsg=new ArrayList<FIFOBroadcastMessage>();
	}
	
	
	public Object clone(){
		FIFOBroadcast fifob = null;
		try { fifob = (FIFOBroadcast) super.clone();
			fifob.next=new int[Network.size()];
			fifob.pendMsg=new ArrayList<FIFOBroadcastMessage>();
		}
		catch( CloneNotSupportedException e ) {} // never happens
		return fifob;
	}
	
	
	@Override
	public void processEvent(Node node, int pid, Object event) {
		if(protocol_id != pid){
			throw new RuntimeException("Receive Message for wrong protocol");
		}
		if(event instanceof FIFOBroadcastMessage){
			FIFOBroadcastMessage fifobm=(FIFOBroadcastMessage)event;
			pendMsg.add(fifobm);
			boolean fini=false;
			while(!fini){
				fini=true;
				int i=0;
				for(FIFOBroadcastMessage fifobmbis : pendMsg){
					if(fifobmbis.getIdSrc() ==fifobm.getIdSrc() && fifobmbis.getNumseq() == next[(int)fifobm.getIdSrc()] ){
						deliver(node,fifobmbis);
						next[(int)fifobm.getIdSrc()]++;
						pendMsg.remove(i);
						fini=false;
						break;
					}
					i++;
				}
				
			}
			
		}

	}

	@Override
	public void broadcast(Node src, Message m) {
		BroadcastProtocol b= (BroadcastProtocol) src.getProtocol(broadcast_protocol_id);
		b.broadcast(src, new FIFOBroadcastMessage(src.getID(), -2, cur_num_seq,"FIFObroadcast", m, protocol_id));
		cur_num_seq++;

	}

	@Override
	public void deliver(Node host, Message m) {
		int pid_dessus=((Message)m.getContent()).getPid();
		((EDProtocol)host.getProtocol(pid_dessus)).processEvent(host, pid_dessus, m.getContent()); 
	}

}
