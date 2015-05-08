set xrange[0:5340]
set xlabel "Temps(s)"
set ylabel "Neurons"
plot "neurons.dat" using 1:2 pt 7 title "Activite des neurons"
