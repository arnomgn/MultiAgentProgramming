package agents;

import java.io.IOException;
import java.security.acl.Acl;
import java.util.ArrayList;

import org.json.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.gson.Gson;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import models.Cell;
import models.CellsBag;
import models.Constants;
import models.Utils;

public class AnalyserAgent extends Agent {
	public static String ASK_ENVIRONMENT_MONSTER_POSITION_CONVID = "askEnvMonstPos";

	protected int value;

	protected void setup() {
		Utils.register(this, this.getLocalName());
		System.out.println("### " + getLocalName() + " is now ... Installed !");
		// set value to agent
		Object[] args = getArguments();
		this.value = (int) args[0];
		//		this.monsterLastPosition = new Cell(-1, -1, -1);
		// add behaviours
		addBehaviour(new SubscribeToArtificialIntelligenceBehaviour());
		addBehaviour(new ReceiveCallForProposalBehaviour());

	}

	public void setValue(int newValue) {
		this.value = newValue;
	}

	public int getValue() {
		return this.value;
	}

	public ArrayList<Cell> getPossiblePosition(Cell monsterLastPosition){

		ArrayList<Cell> tab = new ArrayList();
		if (monsterLastPosition != null) {
			int x = monsterLastPosition.nligne;
			int y = monsterLastPosition.ncolonne;
			int ligne, colonne;
			int i = x-2;
			int j = y-2;
			for (i = x-2; i<=x+2; i++){
				ligne = Math.floorMod(i,Constants.DIM_GRID_X);
				for (j = y-2; j<=y+2 ; j++){
					colonne = Math.floorMod(j,Constants.DIM_GRID_Y);
					Cell cell = new Cell(0,ligne,colonne);
					tab.add(cell);
					//System.out.println(i + "," +j);
				}
			}
		}
		return tab;
	}

	/**
	 * TODO
	 * Behaviour to subscribe to IA Agent
	 */
	//TODO : Aghiles à corriger 
	private class SubscribeToArtificialIntelligenceBehaviour extends OneShotBehaviour {

		@Override
		public void action() {
			AID ai = Utils.searchForAgent(myAgent, Constants.AI_DESCRIPTION);
			// should send a subscribe message to simulation Agent
			ACLMessage subscribeMessage = new ACLMessage(ACLMessage.SUBSCRIBE);
			// add engine  as a receiver
			subscribeMessage.addReceiver(ai);
			subscribeMessage.setContent(myAgent.getLocalName());
			// send message to engine
			send(subscribeMessage);
			System.out.print("\nAgent " + myAgent.getLocalName() + " has just sent a SubscribeToSimulater message to " + ai.getName());
		}
	}
	
	/**
	 * MySequentialBehaviour construit la logique entière de cet agent.
	 * De manière séquentiel, il envoie une requete a environnement
	 * et attend cycliquement sa réponse dans un autre behaviour
	*/
	private class MySequentialBehaviour extends SequentialBehaviour {
		
		public MySequentialBehaviour() {
			addSubBehaviour(new RequestMonsterPositionBehaviour());
			addSubBehaviour(new WaitForMonsterPositionBehaviour());
		}
		
	}


	/**
	 * Behaviour to ask the environment the position of the monster related.
	 * The analyse agent send a message to the environment to execute a fonction find_position()
	 * On its reception, The analyse will have to run a fonction to predicate a critique zone
	 * Then the new position is sent to the environment.
	 */
	private class RequestMonsterPositionBehaviour extends OneShotBehaviour {

		@Override
		public void action() {
			AID environment = Utils.searchForAgent(myAgent, Constants.ENVIRONMENT_DESCRIPTION);
			// should send a subscribe message to simulation Agent
			ACLMessage subscribeMessage = new ACLMessage(ACLMessage.QUERY_REF);
			// add conversation ID
			subscribeMessage.setConversationId(Constants.ANALYSER_ENV_CONVERSATION_ID);
			// add engine  as a receiver
			subscribeMessage.addReceiver(environment);
			subscribeMessage.setContent(Integer.toString(value));
			//System.out.println("AskForMonsterPosition");
			// send message to engine
			send(subscribeMessage);
			// System.out.print("\nAgent " + myAgent.getLocalName() + " has just sent a SubscribeToSimulater message to " + environment.getName());
		}	
	}

	private class WaitForMonsterPositionBehaviour extends OneShotBehaviour {

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM)
					.MatchConversationId(Constants.ANALYSER_ENV_CONVERSATION_ID);
			ACLMessage message ;
			if((message= receive(mt)) ==null)
			{
				block();
				return;
			}
			//System.out.print("WaitForMonsterPositionBehaviour " );
			//System.out.println(message.getContent());
			Gson gson = new Gson();
			Cell monsterCell = gson.fromJson(message.getContent(), Cell.class);
			// prepare possible value
			ArrayList<Cell> cellsTab = ((AnalyserAgent) myAgent).getPossiblePosition(monsterCell);
			try {
				ACLMessage possibleNextPosition = new ACLMessage(ACLMessage.PROPOSE);
				possibleNextPosition.addReceiver(Utils.searchForAgent(myAgent, Constants.AI_DESCRIPTION));
				ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
				String jsonCellsTab = ow.writeValueAsString(cellsTab);
				possibleNextPosition.setContent(jsonCellsTab);
				possibleNextPosition.setConversationId(Constants.ANALYSER_AI_CONVERSATION_ID);
				// replying with new best position
				send(possibleNextPosition);
				cellsTab.clear();
				cellsTab = null;
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}



	private class ReceiveCallForProposalBehaviour extends CyclicBehaviour {

		@Override
		public void action() {
			//First we wait for a message of type "Call for Proposal" from IAAgent
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP).MatchConversationId(Constants.ANALYSER_AI_CONVERSATION_ID);
			ACLMessage message;

			if ((message = myAgent.receive(mt)) == null) {
				block();
				return;
			}
			myAgent.addBehaviour(new MySequentialBehaviour());


		}
	}
}