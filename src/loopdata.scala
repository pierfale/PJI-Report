lines.foreach { l => //pour chaque lignes du fichier de log
      val fields = l split "\t "
      val index = (fields(0).charAt(3).asDigit).toInt + ((fields(0).charAt(1).asDigit).toInt * sizeOfLayer) //index du neuron
        if (listNeurons.contains(index)) {
          if (timeNeurons.get(fields(1)) != None)
            timeNeurons += (fields(1)  -> 
            (timeNeurons.get(fields(1)).get :+ (index)))//ajout dans la list 
          else
            timeNeurons += (fields(1) ->  List(index))//ou creation de list
          if (maxTime < fields(1).toInt)//variable qui sert a cree les bornes du fichier plot
            maxTime = fields(1).toInt
        }
    }
	.
	.
	.
/**
* Creation d'une chaine de caractere pour le fichier de donnees
*/
var str = ""
timeNeurons.keys.foreach { k =>
	(timeNeurons get k).get.foreach { z =>
        str += k + "\t" + z + "\n"
    }
}
   
/**
* Creation du fichier de donnees
*/
Some(new PrintWriter("output/log/neurons.dat")).foreach{p => p.write(str); p.close}