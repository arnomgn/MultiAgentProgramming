package agents;

import org.json.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.gson.Gson;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import models.*;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

/* Traveler Agent is an independant agent.
 * It will simulate a real player moving and trying to survive
 * Its intelligence will be based on a continuous call to AI agents
 * It will then be able to find the safest place to go on the grid.
*/
public class TravelerAgent extends Agent {
	protected AID[] receiver;
	protected int value = Constants.TRAVELER_VALUE;
	public Cell position;
	public Cell oldPosition;

	protected void setup() {
		Utils.register(this, this.getLocalName());
		System.out.println("### " + getLocalName() + " is now ... Installed !");
		// set value to agent
		// setup random position in grid
		this.oldPosition = null;
		this.position = new Cell(this.value, (int)Constants.DIM_GRID_X/2, (int)Constants.DIM_GRID_Y/2);
		// add behaviours
		addBehaviour(new RequestBestMoveBehaviour(this, 2000));
		addBehaviour(new ForwardAIinfoToEnvironmentBehaviour());
	}
	
	/* @todo  This function move the traveler to its new Position
	 * bestPosition is an intelligent move received from AI agent
	 */
	public void move(Cell bestPosition) {
		this.oldPosition = null;
		this.oldPosition = this.position;
		// erasing traveler at its previous position
		this.oldPosition.setValue(0);
		this.oldPosition.setOldValue(this.value);
		// moving to a new intelligent position
		this.position = bestPosition;
		this.position.setValue(this.value);
		//System.out.print("\nAgent " + myAgent.getLocalName() + " has just received a request to move ---> " + newPosition.nligne + "," + newPosition.ncolonne);
	}
	
	
	/**
	 * RequestBestMoveBehaviour 
	 * On each tick, ask for ArtificialInteliigenceAgent the best position to move.
	 * @ For now, just send its new position to environment.
	 */
	private class RequestBestMoveBehaviour extends TickerBehaviour {
		
		public RequestBestMoveBehaviour(Agent a, long period) {
			super(a, period);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void onTick() {
			int i;
			// TODO Auto-generated method stub
			System.out.println("\n### Traveler moved");
			// search for AI agent 
			AID AIAgent = Utils.searchForAgent(myAgent, Constants.AI_DESCRIPTION);
			// @todo : ask for IA a move
			
			try {
				ACLMessage myPositionMessage = new ACLMessage();
				// add performative
				myPositionMessage.setPerformative(ACLMessage.REQUEST);
				// set conversationId 
				myPositionMessage.setConversationId(Constants.TRAVELER_AI_CONVERSATION_ID);
				// add new position as content in json
				ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
				Cell myAgentPosition = ((TravelerAgent)myAgent).position;
				String jsonCells = ow.writeValueAsString((myAgentPosition));
				myPositionMessage.setContent(jsonCells);
				// add AI as a receiver
				myPositionMessage.addReceiver(AIAgent);
				// send message to AI Agent
				send(myPositionMessage);
				//System.out.print("\nAgent " + myAgent.getLocalName() + " has just sent ");
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * GetInformedFromAIBehaviour get a new best position
	 * Move to this position
	 * Forward new position to environment
	 */
	private class ForwardAIinfoToEnvironmentBehaviour extends CyclicBehaviour {

		@Override
		public void action() {
			// should receive a message that match console jade template : INFORM and ConversationId
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM).MatchConversationId(Constants.TRAVELER_AI_CONVERSATION_ID);
			ACLMessage message = myAgent.receive(mt);
			
			if (message != null) {
				try {
					// search for environment 
					AID environment = Utils.searchForAgent(myAgent, Constants.ENVIRONMENT_DESCRIPTION);
					// parse json message with new position
					String jsonMessage = message.getContent(); // chaîne JSON
					Gson gson = new Gson();
					Cell newPosition = gson.fromJson(jsonMessage, Cell.class);
					((TravelerAgent)myAgent).move(newPosition);
					
					// send traveler current CellsBag to Environment
					ACLMessage updatedPositionMessage = new ACLMessage();
					// add receiver
					updatedPositionMessage.addReceiver(environment);
					// add performative
					updatedPositionMessage.setPerformative(ACLMessage.INFORM);
					// add conversationID
					updatedPositionMessage.setConversationId(Constants.TRAVELER_ENV_CONVERSATION_ID);
					// add new position as content in json
					ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
					CellsBag cellsbag = new CellsBag(((TravelerAgent)myAgent).oldPosition, ((TravelerAgent)myAgent).position);
					String jsonCellsbag = ow.writeValueAsString(cellsbag);
					//System.out.println(jsonCellsbag);
					updatedPositionMessage.setContent(jsonCellsbag);
					// replying with new cellsbag
					send(updatedPositionMessage);
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				block();
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
					System.out.print("\n\n --- I lost this game");
					myAgent.doDelete();
				} else {
					block();
				}
			}
		}
	
}
		
		

