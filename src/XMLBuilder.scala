package fr.cristal.emeraude.n2s3.features.io

import java.nio.ByteBuffer
import scala.xml.XML
import java.nio.ByteOrder
import fr.cristal.emeraude.n2s3.core
import fr.cristal.emeraude.n2s3.core.synchronizer.MonoSynchronizer

//for testing, this extends App
abstract class XMLBuilder {

  def load(pathname : String) : core.Network = {
    
    println("Load xml network : \""+pathname+"\"")
    
    val xml =  XML.loadFile(pathname)
    
    // Get neurons per layer list
    val nLayersListTag = (xml\\ "nLayers")
    if(nLayersListTag.size != 1) {
      throw new RuntimeException("Require \"nLayers\" tag, the number of layers described in this file")
    } 
    val nbLayers = Integer.parseInt(nLayersListTag(0) text)
    
    val neuronsByLayers = scala.collection.mutable.ListBuffer.fill(nbLayers)(0)
    (xml \\ "Layer").foreach { layerTag =>
      val indexListTag = (layerTag \\ "index")
      if(indexListTag.size != 1) {
        throw new RuntimeException("Require an unique \"index\" tag for each layer")
      } 
      val layerIndex =  Integer.parseInt(indexListTag(0) text)
      
      val nUnitsTagList = (layerTag \\ "nUnits")
      if(nUnitsTagList.size != 1) {
        throw new RuntimeException("Require an unique \"nUnits\" tag for each layer, which is the number of neurons in this layer")
      } 
      val nbNeurons =  Integer.parseInt(nUnitsTagList(0) text)
      
      neuronsByLayers(layerIndex) = nbNeurons
    }
    
    // create the network
    val network = createNetwork(neuronsByLayers)
    
    network.initiateNetworkWithManualConnection(new MonoSynchronizer())
    
    // set network configuration
    println("Setting network...")
    val decoder = new sun.misc.BASE64Decoder()
    (xml \\ "Layer").foreach { layerTag =>
      
      val layerIndex =  Integer.parseInt((layerTag \\ "index")(0) text)
      
      // Layer target
      val targTAgList = (layerTag \\ "targ")
      if(targTAgList.size != 1) {
        throw new RuntimeException("Require an unique  \"targ\" tag for each layer (miss tag in layer "+layerIndex+" )")
      }
      val targTag = targTAgList(0) text
      val targetIndex = if(targTag.equals("-")) -1 else Integer.parseInt(targTag)
      
      // Neurons threshold
      val threshTagList = (layerTag \\ "thresh")
      if(threshTagList.size != 1) {
        throw new RuntimeException("Require an unique  \"thresh\" tag for each layer (miss tag in layer "+layerIndex+" )")
      }
      val thresholdTag = threshTagList(0)
      
      val thresholdValue = ByteBuffer.wrap(decoder.decodeBuffer( (scala.xml.Utility.trim(thresholdTag) text)) ).order(ByteOrder.LITTLE_ENDIAN).getFloat
      List.range(0, neuronsByLayers(layerIndex)).foreach(currentNeuron => setNeuronThreshold(network, layerIndex , currentNeuron, thresholdValue))
    
      // Synapses weight
      val wTagList = (layerTag \\ "W")
      if(wTagList.size != 1) {
        throw new RuntimeException("Require an unique \"W\" tag for each layer (miss tag in layer "+layerIndex+" )")
      }
      val weightTag = wTagList(0)
      
      if(!(scala.xml.Utility.trim(weightTag) text ).equals("") && targetIndex != -1) {
        
        val inputNeuronsNumber = neuronsByLayers(layerIndex)
        val outputNeuronsNumber = neuronsByLayers(targetIndex)
        
        val weightBuffer = ByteBuffer.wrap( decoder.decodeBuffer((scala.xml.Utility.trim(weightTag) text)) ).order(ByteOrder.LITTLE_ENDIAN)
        var currentInputIndex = 0;
        var currentOutputIndex = 0;

        var conn = Seq[(Int,Int,Int,Int)]()
        var weights = Seq[Float]()
        
        while (weightBuffer.remaining() > 0) {
          
          if(currentInputIndex >= inputNeuronsNumber)
            throw new RuntimeException( "Too much synaptic weight provided ("+(currentInputIndex * outputNeuronsNumber + currentOutputIndex)+")" )
          
          conn = conn :+ ((layerIndex, currentInputIndex, targetIndex, currentOutputIndex))
          weights = weights :+ weightBuffer.getFloat
          
          //TODO increment input or output index first (depend of synapses weight order)
          currentOutputIndex = currentOutputIndex+1;
          if(currentOutputIndex >= outputNeuronsNumber) {
            currentInputIndex = currentInputIndex+1;
            currentOutputIndex = 0
          }
        }
        
        println("Creating synapses on layer : " + layerIndex + "...")
        network.createManualConnection(conn)
        
        var i = 0
        conn.foreach { index =>
          setSynapticWeight(network, index._1 ,index._2, index._3, index._4,  weights(i))
          i += 1
        }
        
        if(currentInputIndex != inputNeuronsNumber)
          throw new RuntimeException("Not enough synaptic weight provided")
      }
    }
    network
	}
  
  /**
   * Abstract method which create network with list of neurons per layer
   */
	def createNetwork(neuronsPerLayer: Seq[Int]) : core.Network;
  
  /**
   * Abstract methods which initialize neuron threshold on current created network
   */
  def setNeuronThreshold(network : core.Network, layer : Int, neuron : Int, threshold : Float) : Unit;
  
    /**
   * Abstract methods which initialize synapse weight on current created network
   */
	def setSynapticWeight(network : core.Network, inputLayer : Int, inputNeuronIndex : Int, outputLayer : Int , outputNeuronIndex : Int, weight : Float) : Unit;


}