package agents;

import java.io.IOException;
import java.io.Serializable;
import java.util.Random;
import models.*;
//import sun.util.calendar.LocalGregorianCalendar.Date;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.leap.ArrayList;
import org.codehaus.jackson.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;



public class Engine extends Agent {
	public ArrayList mySubscriptions = new ArrayList();

	protected void setup() {
		Utils.register(this, this.getLocalName());
		System.out.println("### " + getLocalName() + " is now ... Installed !");
		addBehaviour(new MySequentialBehaviour(this.mySubscriptions));
	}


	/**
	 * MySequentialBehaviour construit la logique entière de cet agent.
	 * De manière séquentiel, il attend que les monster soient inscris.
	 * Puis, il déclenche le OneShot Ticker Behaviour. Finalement, il attend cycliquement la notification
	 * de l'agent environment pour mettre fin au Sudoku.
	 */
	private class MySequentialBehaviour extends SequentialBehaviour {
		
		public MySequentialBehaviour(ArrayList agentSubscriptions) {
			System.out.println("### Beginning the main game logic ...  ");
			addSubBehaviour(new AcceptNewSubscriptionBehaviour(agentSubscriptions));
			addSubBehaviour(new myTickerBehaviour(myAgent, 1000, agentSubscriptions));
			addBehaviour(new WaitForEndOfGameBehaviour());
		}
		
	}
	
	/**
	 * AcceptNewSubscriptionBehaviour accepte et inscrit les nouveaux agents Monster lorsqu'il est notifié
	 * par eux. 
	 */
	private class AcceptNewSubscriptionBehaviour extends Behaviour {
		public ArrayList myAgentSubscriptions;
		
		public AcceptNewSubscriptionBehaviour(ArrayList agentSubscriptions) {
			System.out.println("### Waiting for monsters' subscription ...  ");
			this.myAgentSubscriptions =  agentSubscriptions;
		}
		
		@Override
		public boolean done() {
			if (myAgentSubscriptions.size() >= Constants.MONSTER_NUMBER) {
				System.out.println("\n### Waiting for monsters' subscription ... done !" + "\n ==> Size is " + myAgentSubscriptions.size());
				System.out.println("\n### Beginning engine ticker...  ");
				return true;
			} else return false;
		}

		@Override
		public void action() {
			// should receive a message that match console jade template : SUBSCRIBE
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE);
			ACLMessage message = myAgent.receive(mt);
			
			if (message != null) {
				System.out.print("\nAgent " + myAgent.getLocalName() + " has just received message --- " + message.getContent());
				AID senderAID = message.getSender();
				// inserting new AID in Simulation Agent
				if (this.insertNewSubscription(senderAID)) {
					System.out.print("\nAgent " + myAgent.getLocalName() + " has just subscribed --- " + message.getSender().getName());
				} else {
					// simulater capacity reached
					System.out.print("\nERROR --- on" + myAgent.getLocalName() +  ": subscriptions max capacity reached");
					block();
				}
			} else {
				block();
			}
		}
		
		public boolean insertNewSubscription(AID agentAID) {
			if (mySubscriptions.size() > Constants.MONSTER_NUMBER) {
				return false;
			}
			mySubscriptions.add(agentAID);
			return true;
		}
	}
	
	/**
	 * TickerBehaviour doit envoyer à intervalles réguliers des notifications à l'environnement avec
	 * le AID des monster. 
	 */
	private class myTickerBehaviour extends TickerBehaviour {
		ArrayList superAgentSubscription;
		
		public myTickerBehaviour(Agent a, long period, ArrayList agentSubscriptions) {
			super(a, period);
			// TODO Auto-generated constructor stub
			this.superAgentSubscription = agentSubscriptions;
		}

		@Override
		protected void onTick() {
			int i;
			// TODO Auto-generated method stub
			//System.out.println("\n### Tic");
			// search for environment 
			AID environment = Utils.searchForAgent(myAgent, Constants.ENVIRONMENT_DESCRIPTION);
			for(i = 0; i < Constants.MONSTER_NUMBER; i ++) {
				try {
					// send 27 analyser AID to environment agent
					ACLMessage informMessage = new ACLMessage();
					// set AID content formated in json
					ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
					String json = ow.writeValueAsString(this.superAgentSubscription.get(i));
					informMessage.setContent(json);
					// add performative
					informMessage.setPerformative(ACLMessage.REQUEST);
					// add envir as a receiver
					informMessage.addReceiver(environment);
					// add analyser AID
					// send message to environment Agent
					send(informMessage);
					//System.out.print("\nAgent " + myAgent.getLocalName() + " has just sent " + this.superAgentSubscription.get(i).toString() + " to " + environment.getName());
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	private class WaitForEndOfGameBehaviour extends CyclicBehaviour {
		
		@Override
		public void action() {
			// should receive a message that match console jade template : INFORM
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage message = myAgent.receive(mt);
			
			if (message != null) {
				System.out.print("\nAgent " + myAgent.getLocalName() + " has just received message --- " + message.getContent());
				System.out.print("\n\n --- end of game");
				myAgent.doDelete();
			} else {
				block();
			}
		}
	}
}

