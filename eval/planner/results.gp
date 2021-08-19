set datafile separator comma

plot "results.csv" u 1:2:3:4 i 0 w yerrorlines t "HSP", \
                "" u 1:2:3:4 i 1 w yerrorlines t "FF"