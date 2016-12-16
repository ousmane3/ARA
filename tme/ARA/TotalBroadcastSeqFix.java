package tme1shadow;

import java.util.ArrayList;
import java.util.List;

import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;
import tme1.BroadcastProtocol;
import tme1.Message;

public class TotalBroadcastSeqFix implements BroadcastProtocol, EDProtocol {

	
	private static final String PAR_TRANSPORT = "transport";
	
	private static final String PAR_BROADCASTID = "broadcast";
	
	private static final String PAR_SEQUENCEURID = "sequenceur";
	
	private final int protocol_id;
	private final int broadcast_protocol_id;
	private final int transport_id;
	private final long sequenceur_id;
	
	private  int cur_num_seq=0;//pour le sequenceur
	
	private  int nextdelv=0;
	private List<TotalBroadcastSeqFixMessage> pendMsg;
	
	public TotalBroadcastSeqFix(String prefix){
		String tmp[]=prefix.split("\\.");
		protocol_id=Configuration.lookupPid(tmp[tmp.length-1]);
		broadcast_protocol_id=Configuration.getPid(prefix+"."+PAR_BROADCASTID);
		transport_id=Configuration.getPid(prefix+"."+PAR_TRANSPORT);
		sequenceur_id=Configuration.getLong(prefix+"."+PAR_SEQUENCEURID);
		
		pendMsg=new ArrayList<TotalBroadcastSeqFixMessage>();
		
	}
	
	public Object clone(){
		TotalBroadcastSeqFix res = null;
		try { res = (TotalBroadcastSeqFix) super.clone();
			
			res.pendMsg=new ArrayList<TotalBroadcastSeqFixMessage>();
		}
		catch( CloneNotSupportedException e ) {} // never happens
		return res;
	}
	
	
	
	@Override
	public void processEvent(Node node, int pid, Object event) {
		if(protocol_id != pid){
			throw new RuntimeException("Receive Message for wrong protocol");
		}
		if(! (event instanceof TotalBroadcastSeqFixMessage) ){
			throw new RuntimeException("Receive bad type Message for this protocol");
		}
		
		TotalBroadcastSeqFixMessage totalm = (TotalBroadcastSeqFixMessage)event;
		
		if(totalm.getTag().equals("seqtodest")){
			if(totalm.getIdSrc() != sequenceur_id){
				throw new RuntimeException("Total Broadcast : Node"+node.getID()+"received a FIFOmessage from the node "+totalm.getIdSrc()+" not a sequencer");
			}
			pendMsg.add(totalm);
			boolean fini=false;
			while(!fini){
				fini=true;
				int i=0;
				for(TotalBroadcastSeqFixMessage fifobmbis : pendMsg){
					if(fifobmbis.getNumseq() == nextdelv){
						deliver(node,fifobmbis);
						nextdelv++;
						pendMsg.remove(i);
						fini=false;
						break;
					}
					i++;
				}
				
			}
		
		}else if(totalm.getTag().equals("emtoseq")){
			if(node.getID() != sequenceur_id){
				throw new RuntimeException("Total Broadcast : Node "+node.getID()+"received a message instead of sequencer");
			}
			BroadcastProtocol b = (BroadcastProtocol)node.getProtocol(broadcast_protocol_id);
			b.broadcast(node, new TotalBroadcastSeqFixMessage(node.getID(), -2, cur_num_seq, "seqtodest", totalm.getContent(), protocol_id));
			cur_num_seq++;
		}

	}

	@Override
	public void broadcast(Node src, Message m) {
		Transport tr= (Transport) src.getProtocol(transport_id);
		//trouver le node sequenceur
		Node dest=null;
		for(int i=0;i< Network.size();i++){
			if(Network.get(i).getID() == sequenceur_id){
				dest=Network.get(i);
				break;
			}
			
		}
		
		tr.send(src, dest, new TotalBroadcastSeqFixMessage(src.getID(), dest.getID(), -1, "emtoseq", m, protocol_id) , protocol_id);

	}

	@Override
	public void deliver(Node host, Message m) {
		int pid_dessus=((Message)m.getContent()).getPid();
		((EDProtocol)host.getProtocol(pid_dessus)).processEvent(host, pid_dessus, m.getContent()); 

	}

}
