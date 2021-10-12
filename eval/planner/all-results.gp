set xlabel "complexity of assembly model"
set ylabel "execution time (in ms)"

set logscale y

plot "all-results.tsv" u 1:2:3:4 i 0 w yerrorlines t "FF"