package fr.cristal.emeraude.n2s3.core

import akka.actor._
import fr.cristal.emeraude.n2s3.core.exceptions.InputException
import java.util.Arrays

/**
 * An input generator which will send event to network
 * @author bdanglot & falezp
 */
abstract class InputGenerator(network : Network) extends Actor {

  /**
   * Map which associate the index of the input to a list of ActorRef of synapses.
   */
   val connections : scala.collection.mutable.Map[Int,  List[ActorRef]] = new scala.collection.mutable.HashMap[Int, List[ActorRef]]()
  
   initialize()

  /**
   * Return list of connection with input
   * First tuple parameter : number of the input connection
   * Second tuple parameter : target layer
   * Third tuple parameter : target neuron
   **/
  def getInputSynapses() : Seq[(Int,Int, Int)]
  
  /**
   * Called when one or serval inputs event arise
   * @return : An InputEvent send to the Network.
   */
  def provideInputs() : Seq[InputEvent]

  /**
   * @return true if event remain, false otherwise
   */
  def hasNext() : Boolean

  /**
   * Initialization of the input synapses(synapses between this InputGenerator and
   * first layer of neurons) and connect them to the network.
   * Using reportAndInformNeurons() method from core.Network.
   */
  def initialize() : Unit = {
    /* create synapses to connect input to the network */
    getInputSynapses().foreach(
      connection => {
        if (!connections.contains(connection._1))
          connections += (connection._1 -> List())
        
        connections(connection._1) = (network.system.actorOf(Props(network.createSynapse(self, network.neuronsActorRef(connection._2)(connection._3), network.synchronizer.getSynchronizerOfSynapse(-1, connection._1, connection._2, connection._3))
        ), name = "SInput"+connection._1+"-"+connection._2+","+connection._3) :: connections(connection._1))
      }
    )
    /* tell to input about their new friends : synapses */
    connections.foreach( synapse =>
      network.reportAndInformNeurons(synapse._2)
    )
  }

  /**
   * Call processAll() if a message InputStart is received
   * @throw UnknownMessageInputException otherwise.
   */
  def receive = {
    case ImYourChild(_) => ()
    case TriggerInput => {
      if(hasNext()) {
        provideInputs().foreach { event => network.sendInputEvent(event) }
      }
      else 
        sender ! Stop
    }
  }
}