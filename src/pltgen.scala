/**
* Creation d'une chaine pour le fichier plot (neurons.plt)
*/
str = ""
if (options.get("time") != None)
	str += "set xrange[0:"+((options.get("time").get))+"]\n"
else 
	str += "set xrange[0:"+(maxTime+100)+"]\n"
	str += "set xlabel \"Temps(s)\"\nset ylabel \"Neurons\"\n"
	str += "plot \"neurons.dat\" using 1:2 pt 7 title \"Activite des neurons\"\n"