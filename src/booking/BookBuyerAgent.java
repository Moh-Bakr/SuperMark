package booking;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.Vector;

public class BookBuyerAgent extends Agent {
	private static final long serialVersionUID = 1L;
        Vector history;
	private String targetBookTitle;
	private AID[] sellerAgents;
	private BookBuyerGui myGui;
	protected void setup() {
		System.out.println("Hello! Buyer-agent "+getAID().getName()+" is ready.");
              myGui = new BookBuyerGui(this);
	      myGui.showGui();
              history = new Vector();
              System.out.print(myGui.titleField.getText());
              
               targetBookTitle = null;
                addBehaviour(new TickerBehaviour(this,20000){
                    @Override
                    protected void onTick() {
		 addBehaviour(new transaction());
                    }
                
                });
		
	}

	protected void takeDown() {
	}

   void setHistory(String bestSelleragent) {
         addBehaviour(new OneShotBehaviour() {
             private static final long serialVersionUID = 1L;

			public void action() {
             history.add("get "+targetBookTitle+" from "+bestSelleragent);
         }});
   
   }
     Vector getHistory() {
           return history;
    }

    void sendTargetBook(String book) {
        addBehaviour(new OneShotBehaviour() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void action() {
				targetBookTitle = book.toString();
				System.out.println(targetBookTitle  );
			}
		});
     
    }

    private   class transaction extends CyclicBehaviour {

        public transaction() {
        }

        @Override
        public void action() {

if (targetBookTitle != null) {
                    System.out.println(targetBookTitle);
			  
			System.out.println("Target book is "+targetBookTitle);

		 
 			 
					System.out.println("Trying to buy "+targetBookTitle);
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType("book-selling");
					template.addServices(sd);
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template); 
						System.out.println("Found the following seller agents:");
						sellerAgents = new AID[result.length];
						for (int i = 0; i < result.length; ++i) {
							sellerAgents[i] = result[i].getName();
							System.out.println(sellerAgents[i].getName());
						}
					}
					catch (FIPAException fe) {
						fe.printStackTrace();
					}

					myAgent.addBehaviour(new RequestPerformer());
				}
			 
		}
		 
        
    }

	private class RequestPerformer extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		private AID bestSeller; 
		private int bestPrice;  
		private int repliesCnt = 0; 
		private MessageTemplate mt; 
		private int step = 0;

		public void action() {
                    if(targetBookTitle!=null){
			switch (step) {
			case 0:
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < sellerAgents.length; ++i) {
					cfp.addReceiver(sellerAgents[i]);
				} 
				cfp.setContent(targetBookTitle);
				cfp.setConversationId("book-trade");
				cfp.setReplyWith("cfp" + System.currentTimeMillis()); 
				myAgent.send(cfp);
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						int price = Integer.parseInt(reply.getContent());
						if (bestSeller == null || price < bestPrice) {
							bestPrice = price;
							bestSeller = reply.getSender();
						}
					}
					repliesCnt++;
					if (repliesCnt >= sellerAgents.length) {
						step = 2; 
					}
				}
				 
				break;
			case 2:
				ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				order.addReceiver(bestSeller);
				order.setContent(targetBookTitle);
				order.setConversationId("book-trade");
				order.setReplyWith("order"+System.currentTimeMillis());
				myAgent.send(order);

				mt = MessageTemplate.and(
						MessageTemplate.MatchConversationId("book-trade"),
						MessageTemplate.MatchInReplyTo(order.getReplyWith()));
				step = 3;
				break;
			case 3:      
				
				reply = myAgent.receive(mt);
				if (reply != null) {
					if (reply.getPerformative() == ACLMessage.INFORM) {
						System.out.println(targetBookTitle
								+ " successfully purchased from agent "
								+ reply.getSender().getName());
						System.out.println("Price = " + bestPrice);
                                                setHistory( reply.getSender().getName());
                                              targetBookTitle = null;
						 
					}
					else {
						System.out.println("Attempt failed: requested book already sold.");
					}

					step = 4;
				}
				 
				break;
			}        
		}
                }
                
	}
        
        
}
