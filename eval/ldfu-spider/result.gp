set datafile separator comma

plot "result.csv" u (0):3 i 0 w boxplot t "Hypermedea", \
               "" u (1):3 i 1 w boxplot t "Linked-Data-Fu"