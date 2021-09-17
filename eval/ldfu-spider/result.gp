set datafile separator comma

plot "result.csv" u (0):2 i 0 w boxplot t "Hypermedea", \
               "" u (1):2 i 1 w boxplot t "Linked-Data-Fu"