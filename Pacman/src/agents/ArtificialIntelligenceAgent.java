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
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SequentialBehaviour;
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
import jade.util.leap.HashMap;


public class ArtificialIntelligenceAgent extends Agent {
	protected int numberProposeReceived;
	protected ArrayList<Cell> predictedMonsterPositionsList = new ArrayList<>();
	protected ArrayList<AID> analysersSubscriptionsList = new ArrayList<>();
	protected Cell travelerPosition = null;
	protected boolean readyForAnalyse = false;
	protected int analyseReceived = 0;

	Grid grid = new Grid();

	protected void setup() {
		Utils.register(this, this.getLocalName());
		System.out.println("### " + getLocalName() + " is now ... Installed !");
		numberProposeReceived = 0;
		addBehaviour(new MySequentialBehaviour(analysersSubscriptionsList));
	}

	public boolean positionCorrect(int i, int j, Grid grid) {
		return grid.getObtacles(i,j);
	}
	
	public Cell chooseBestMove(){
		
		//First :trouver la position menacante la plus proche
		
		double bestdist =  Math.pow(predictedMonsterPositionsList.get(0).nligne - travelerPosition.nligne, 2)  +  Math.pow(predictedMonsterPositionsList.get(0).ncolonne - travelerPosition.ncolonne, 2);
		int bestIndex  = 0;
		for (int i = 1; i< predictedMonsterPositionsList.size();  i++){
			// On récupère la cell 
			Cell cell = predictedMonsterPositionsList.get(i);
			// On calcule la distance euclidienne
			double dist = Math.pow(cell.nligne - travelerPosition.nligne, 2)  + Math.pow(cell.ncolonne- travelerPosition.ncolonne,2);
			if (dist < bestdist){bestdist = dist; bestIndex = i;}
		}
		// Now evaluate the best position to be far from bestdist
		ArrayList<Cell> nextPositionTraveller = new ArrayList<>();
		//top
		int ligne = travelerPosition.nligne;
		ligne = Math.floorMod(ligne-1, Constants.DIM_GRID_X);
		Cell topPosition = new Cell (0,ligne,travelerPosition.ncolonne);
		System.out.print("position correct ? :");
		System.out.print(positionCorrect(topPosition.nligne, topPosition.ncolonne, grid));
		if (positionCorrect(topPosition.nligne, topPosition.ncolonne, grid) == false){nextPositionTraveller.add(topPosition);}
		//Bottom
		ligne = travelerPosition.nligne;
		ligne = Math.floorMod(ligne+1, Constants.DIM_GRID_X);
		Cell bottomPosition = new Cell (0,ligne,travelerPosition.ncolonne);
		if (positionCorrect(bottomPosition.nligne, bottomPosition.ncolonne, grid) == false){nextPositionTraveller.add(bottomPosition);}
		//right
		int colonne = travelerPosition.ncolonne;
		colonne = Math.floorMod(colonne+1, Constants.DIM_GRID_Y);
		Cell rightPosition = new Cell (0,travelerPosition.nligne,colonne);
		if (positionCorrect(rightPosition.nligne, rightPosition.ncolonne, grid) == false){nextPositionTraveller.add(rightPosition);}
		//left 
		colonne = travelerPosition.ncolonne;
		colonne = Math.floorMod(colonne-1, Constants.DIM_GRID_Y);
		Cell leftPosition = new Cell (0,travelerPosition.nligne,colonne);
		if (positionCorrect(leftPosition.nligne, leftPosition.ncolonne, grid) == false){nextPositionTraveller.add(leftPosition);}
		
		// Now, we choose the farest 
		int indexToChoose = 0;
		Cell dangerousCell = predictedMonsterPositionsList.get(bestIndex);
		double distToChoose =  Math.pow(nextPositionTraveller.get(0).nligne - predictedMonsterPositionsList.get(bestIndex).nligne, 2) 
				+ Math.pow(nextPositionTraveller.get(0).ncolonne - predictedMonsterPositionsList.get(bestIndex).ncolonne, 2);
		
		for (int k = 1; k< nextPositionTraveller.size(); k++){
			Cell currentCell = nextPositionTraveller.get(k);
			double dist = Math.pow(dangerousCell.nligne - currentCell.nligne, 2)
					+ Math.pow(dangerousCell.ncolonne - currentCell.ncolonne, 2);
			if (dist > distToChoose){distToChoose = dist;indexToChoose = k;}
		}
		
		return nextPositionTraveller.get(indexToChoose);
			
		}
		
	
	/**
	 * MySequentialBehaviour construit la logique entiÃ¨re de cet agent.
	 * De maniÃ¨re sÃ©quentiel, il attend que les analyser soient inscris.
	 * Puis, il dÃ©clenche les Cyclicls behaviour en parallele
	*/
	private class MySequentialBehaviour extends SequentialBehaviour {
		
		public MySequentialBehaviour(ArrayList agentSubscriptions) {
			addSubBehaviour(new AcceptNewSubscriptionBehaviour(agentSubscriptions));
			addSubBehaviour(new MyParallelLogicBehaviour());
		}
		
	}

	/**
	 * AcceptNewSubscriptionBehaviour accepte et inscrit les nouveaux agents Monster lorsqu'il est notifiÃ©
	 * par eux. 
	*/
	private class AcceptNewSubscriptionBehaviour extends Behaviour {
		public ArrayList myAgentSubscriptions;
		
		public AcceptNewSubscriptionBehaviour(ArrayList agentSubscriptions) {
			System.out.println("### Waiting for analyser' subscription ...  ");
			this.myAgentSubscriptions =  agentSubscriptions;
		}
		
		@Override
		public boolean done() {
			if (myAgentSubscriptions.size() >= Constants.MONSTER_NUMBER) {
				System.out.println("\n### Waiting for analyser' subscription ... done !" + "\n ==> Size is " + myAgentSubscriptions.size());
				return true;
			} else return false;
		}

		@Override
		public void action() {
			// should receive a message that match console jade template : SUBSCRIBE and conversationID 
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
			if (analysersSubscriptionsList.size() > Constants.MONSTER_NUMBER) {
				return false;
			}
			analysersSubscriptionsList.add(agentAID);
			return true;
		}
	}
	
	/**
	 * Behaviour to trigger both cyclic behaviour
	*/
	private class MyParallelLogicBehaviour extends ParallelBehaviour {
		
		public MyParallelLogicBehaviour() {
			addSubBehaviour(new GetRequestedFromTravelerBehaviour());
			addSubBehaviour(new GetProposalFromAnalyserBehaviour());
		}
	}
		
	/**
	 * GetRequestedFromTravelerBehaviour to wait for traveler request ofbest position
	 * On each request, AI will call for proposal the analyser.
	 * Chose the best option, based on traveler position.
	*/
	private class GetRequestedFromTravelerBehaviour extends CyclicBehaviour {
			
			@Override
			public void action() {
				// should receive a message that match console jade template : REQUEST and conversationId
				MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST).MatchConversationId(Constants.TRAVELER_AI_CONVERSATION_ID);
				ACLMessage message = myAgent.receive(mt);
				
				if (message != null && (((ArtificialIntelligenceAgent)myAgent).analyseReceived == 0)) {
					String jsonMessage = message.getContent(); 
					Gson gson = new Gson();
					Cell travelerPosition = gson.fromJson(jsonMessage, Cell.class);
					((ArtificialIntelligenceAgent)myAgent).travelerPosition = travelerPosition;
					//CONTRACT-NET with all Analysers
					((ArtificialIntelligenceAgent)myAgent).analysersSubscriptionsList.forEach(cle->{
						ACLMessage message_to_analyzer= new ACLMessage(ACLMessage.CFP);
						message_to_analyzer.addReceiver(cle);
						message_to_analyzer.setConversationId(Constants.ANALYSER_AI_CONVERSATION_ID);
						send(message_to_analyzer);
					});
					((ArtificialIntelligenceAgent)myAgent).readyForAnalyse = true;
					((ArtificialIntelligenceAgent)myAgent).analyseReceived = 0;
				} else {
					block();
				}
			}
		}
	
	/**
	 * GetProposalFromAnalyserBehaviour
	 * receive all position and stock it in list
	 * Chose the best option, based on traveler position and predicted position.
	 * send analysed move to traveler
	*/
	public class GetProposalFromAnalyserBehaviour  extends CyclicBehaviour{
		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE).MatchConversationId(Constants.ANALYSER_AI_CONVERSATION_ID);
			ACLMessage message = myAgent.receive(mt);
			if (message != null && (((ArtificialIntelligenceAgent)myAgent).readyForAnalyse)){
				try {
					String jsonMessage = message.getContent(); // chaÃ®ne JSON
					Gson gson = new Gson();
					Cell[] monsterPossiblePosition = (Cell[]) gson.fromJson(jsonMessage, Cell[].class);
					for (int i = 0; i < monsterPossiblePosition.length; i++) { // Loop through every name/phone number combo
						predictedMonsterPositionsList.add(monsterPossiblePosition[i]);
					}
					((ArtificialIntelligenceAgent)myAgent).analyseReceived +=1;
					if (((ArtificialIntelligenceAgent)myAgent).analyseReceived >= Constants.MONSTER_NUMBER) {
						((ArtificialIntelligenceAgent)myAgent).readyForAnalyse = false;
						this.answerTraveler();
						// clean all list for new analyse pattern to be able to start
						((ArtificialIntelligenceAgent)myAgent).predictedMonsterPositionsList.clear();
						((ArtificialIntelligenceAgent)myAgent).analyseReceived =0;
					}
				}catch(Exception e){e.printStackTrace();}
			}
			else{block();}
		}
		
		protected void answerTraveler() {
			try {
				System.out.println("SIZE OF POSSIBLE MOVE = " + predictedMonsterPositionsList.size());
				Cell bestMove;
				if(((ArtificialIntelligenceAgent)myAgent).predictedMonsterPositionsList.size() > 0) {
					bestMove = ((ArtificialIntelligenceAgent)myAgent).chooseBestMove();
				} else {
					// if analyse failed : doesnt move yet 
					bestMove = ((ArtificialIntelligenceAgent)myAgent).travelerPosition;
				}
				ACLMessage bestMoveMessage = new ACLMessage(ACLMessage.INFORM);
				// add receiver
				// search for Traveler agent 
				AID TravelerAgent = Utils.searchForAgent(myAgent, Constants.TRAVELER_DESCRIPTION);
				bestMoveMessage.addReceiver(TravelerAgent);
				// add conversationID
				bestMoveMessage.setConversationId(Constants.TRAVELER_AI_CONVERSATION_ID);
				// add new best position as content in JSON
				ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
				String jsonCell = ow.writeValueAsString(bestMove);
				bestMoveMessage.setContent(jsonCell);
				// replying with new best position
				send(bestMoveMessage);	
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
		}
	}
	
}