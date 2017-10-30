package agents;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Random;

import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.gson.Gson;

import models.*;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
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
import jade.util.leap.ArrayList;


public class Environment extends Agent {
	protected int nshot;
	protected Grid myGrid;
	public int count = 0;

	protected void setup() {
		Utils.register(this, this.getLocalName());
		System.out.println("### " + getLocalName() + " is now ... Installed !");

		this.myGrid = new Grid();
		this.nshot = 0;

		addBehaviour(new GetInformedFromEngineBehaviour(this.myGrid));
		addBehaviour(new GetInformedFromEntitiesBehaviour(this.myGrid));
		addBehaviour(new InformAnalyzerOfMonsterPositionBehaviour());

		this.displayMyGrid();
	}
	
	public void setMyGrid(Grid newGrid) {
		this.myGrid = newGrid;
	}
	
	public Grid getMyGrid() {
		return this.myGrid;
	}
	
	public boolean validMove(Cell targetedCell, Cell oldPosition) {
		boolean inValid = (oldPosition.wasTraveler() && targetedCell.isMonster());
		boolean inValid2 = (oldPosition.wasMonster() && targetedCell.isTraveler());
		
		return (!inValid && !inValid2);
	}
	
	public void updateMyGrid(CellsBag cellsBag) {
		int newPositionLi = cellsBag.newPosition.nligne;
		int newPositionCol = cellsBag.newPosition.ncolonne;
		Cell targetedCell = this.myGrid.getCell(newPositionLi, newPositionCol);
		if (validMove(targetedCell, cellsBag.oldPosition)) {
			cellsBag.oldPosition.setOldValue(0);
			this.myGrid.updateCell(cellsBag.oldPosition);
			this.myGrid.updateCell(cellsBag.newPosition);
		} else {
			this.myGrid.endGame();
			addBehaviour(new EndOfGameBehaviour());
		}
	}

	public void displayMyGrid() {
		System.out.println("\n\n\n\n\n\n\n\n\n----------------------------" + this.nshot + "----------------------------\n\n");
		this.myGrid.display();

		System.out.println("\n\n----------------------------" + this.nshot + "----------------------------");
		this.nshot = this.nshot + 1 ;
	}
	
	public Cell retrieveMonsterPositionUsingValue(Integer value_received) {
		Cell cellule; 
		for(int i = 0; i < Constants.DIM_GRID_X ; i ++) {
			for(int j = 0; j < Constants.DIM_GRID_Y ; j ++) {
				cellule = this.myGrid.grid[i][j];
				if(cellule.isMonster() && cellule.getValue() == value_received)
				{
					//System.out.println("Valeur à tester "+value_received);
					//System.out.println("Monstre trouvé : "+i+" "+j+ " " + this.myGrid.grid[i][j].getValue()+" \n");
					return cellule;
				}
			}
		}
		return null;
	}

	/**
	 * GetInformedFromEngineBehaviour get an agent AID from engine and trigger a request to it directly.
	 */
	private class GetInformedFromEngineBehaviour extends Behaviour {
		Grid superGrid;
		
		public GetInformedFromEngineBehaviour(Grid grid) {
			this.superGrid = grid;
		}
		
		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			if (this.superGrid  != null) {
				return (this.superGrid.isOver());	
			}
			return false;
		}

		@Override
		public void action() {
			// should receive a message that match console jade template : REQUEST
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage message = myAgent.receive(mt);
			
			if (message != null) {
				//display and refresh grid;
				//((Environment)myAgent).displayMyGrid();
				String jsonMessage = message.getContent(); // chaîne JSON
				// parse json message with MonsterX information
				JSONObject obj = new JSONObject(jsonMessage);
				String monsterXLocalName = obj.getString("localName");
				String monsterXName = obj.getString("name");
				AID monsterX = Utils.searchForAgent(myAgent, monsterXLocalName);
				ACLMessage requestMessage = new ACLMessage();
				// add performative
				requestMessage.setPerformative(ACLMessage.REQUEST);
				// add receiver
				requestMessage.addReceiver(monsterX);
				// add conversationID
				requestMessage.setConversationId(Constants.MONSTER_ENV_CONVERSATION_ID);
				// send message
				send(requestMessage);
				// refresh local grid
				this.superGrid = ((Environment)myAgent).getMyGrid();
			} else {
				block();
			}
		}
	}
	
	/**
	 * GetInformedFromEntitiesBehaviour get an updated grid from a monster or the traveler(with its new position) and update UI.
	*/
	private class GetInformedFromEntitiesBehaviour extends Behaviour {
		Grid superGrid;
		
		public GetInformedFromEntitiesBehaviour(Grid grid) {
			this.superGrid = grid;
		}
		
		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			if (this.superGrid  != null) {
				return (this.superGrid.isOver());	
			}
			return false;
		}

		@Override
		public void action() {
			// should receive a message that match console jade template : INFORM and ConversationId 
			MessageTemplate mt1 = MessageTemplate.MatchPerformative(ACLMessage.REQUEST)
					.MatchConversationId(Constants.MONSTER_ENV_CONVERSATION_ID);
			MessageTemplate mt2 = MessageTemplate.MatchPerformative(ACLMessage.REQUEST)
					.MatchConversationId(Constants.TRAVELER_ENV_CONVERSATION_ID);
			
			// either traveler or monster conv_id
			MessageTemplate m1_or_m2 = MessageTemplate.or(mt1, mt2);
			ACLMessage message = myAgent.receive(m1_or_m2);
			
			if (message != null) {
				((Environment)myAgent).count++;
				if (((Environment)myAgent).count == Constants.MONSTER_NUMBER) {
					this.superGrid.display();
					((Environment)myAgent).count = 0;
				}
				String jsonMessage = message.getContent(); // chaîne JSON
				// System.out.println(jsonMessage);
				// parse json message with entities cellsbag
				Gson gson = new Gson();
				CellsBag cellsBag = gson.fromJson(jsonMessage, CellsBag.class);
				//Check validity of new position
				if (myGrid.getObtacles(cellsBag.newPosition.nligne, cellsBag.newPosition.ncolonne)) {
					int value = cellsBag.newPosition.getValue();
					if (myGrid.getObtacles(cellsBag.oldPosition.nligne, cellsBag.oldPosition.ncolonne)) {
						value = -1;
					} 
					// remove old monster value from dirty position
					Cell dirtyCell = new Cell(value,cellsBag.oldPosition.nligne, cellsBag.oldPosition.ncolonne);
					((Environment)myAgent).myGrid.updateCell(dirtyCell);
					// prevent Monster from moving : has to move in a new random one
					ACLMessage errorReply = message.createReply();
					errorReply.setContent("Error");
					errorReply.setPerformative(ACLMessage.FAILURE);
					send(errorReply);
				} else {
					if (myGrid.getObtacles(cellsBag.oldPosition.nligne, cellsBag.oldPosition.ncolonne)) {
						cellsBag.oldPosition.setValue(-1);
					}
					((Environment)myAgent).updateMyGrid(cellsBag);
					this.superGrid = ((Environment)myAgent).getMyGrid();
				}
			} else {
				block();
			}
		}
	}
	
	/**
	 * InformAnalyzerOfMonsterPositionBehaviour
	 * send monster position to Analyser (corresponding to the analyser value).
	*/
	
	public class InformAnalyzerOfMonsterPositionBehaviour extends CyclicBehaviour {

		@Override
		public void action() {
			// TODO Auto-generated method stub
			ACLMessage message_received;
			MessageTemplate mTemplate1 = MessageTemplate.MatchConversationId(Constants.ANALYSER_ENV_CONVERSATION_ID);
			if((message_received = receive(mTemplate1)) == null) {
				block();
				return;
			}
			Integer value_received = Integer.valueOf(message_received.getContent());
			Cell monster_position = ((Environment)myAgent).retrieveMonsterPositionUsingValue(value_received);
			if(monster_position == null){
				System.out.println("No Monster" + value_received + " found, should be temporarily hidden by wall");
			}
			ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
			ACLMessage replyToAnalyzer = message_received.createReply();
			replyToAnalyzer.setPerformative(ACLMessage.INFORM);
			try {
				String jsonMonsterPosition = ow.writeValueAsString(monster_position);
				replyToAnalyzer.setContent(jsonMonsterPosition);
				send(replyToAnalyzer);
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	/**
	 * Behaviour to send an inform message to the engine and traveler (player) when the game is over
	 */
	private class EndOfGameBehaviour extends OneShotBehaviour {
		@Override
		public void action() {
			// search for engine
			AID engine = Utils.searchForAgent(myAgent, Constants.ENGINE_DESCRIPTION);
			// should send a subscribe message to simulation Agent
			ACLMessage endMessage = new ACLMessage();
			// add performative
			endMessage.setPerformative(ACLMessage.INFORM);
			// add receiver
			endMessage.addReceiver(engine);
			// send message 
			send(endMessage);
			System.out.print("\nAgent " + myAgent.getLocalName() + " has just sent an enfOfGame message to " + engine.getName());
			// search for traveler
			AID traveler = Utils.searchForAgent(myAgent, Constants.TRAVELER_DESCRIPTION);
			// should send a subscribe message to simulation Agent
			ACLMessage endMessage2 = new ACLMessage();
			// add performative
			endMessage2.setPerformative(ACLMessage.INFORM);
			// add receiver
			endMessage2.addReceiver(traveler);
			// send message 
			send(endMessage2);
			System.out.print("\nAgent " + myAgent.getLocalName() + " has just sent an enfOfGame message to " + traveler.getName());
		}
	}
}

		

