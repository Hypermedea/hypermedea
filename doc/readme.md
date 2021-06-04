This project aims to create an linked data fu spider artifact usable by MAS agents written in Jacamo that can register ontologies and execute linked data-fu programs to retrieve queries results from integrated data.

A simple use of this artifact is to register owl ontologies, which adds in the belief base of an agent focussed on the artifact observable properties unary binary predicates of A_Box and T_Box axioms, and then the exernal action crawl adds to the belief base all triplets retrieved by the linked data-fu program as well as all unary/binary predicates for properties/classes that has been previously registered.

A base ldfu_agent written in jacamo with plans for using the external actions of the artifact as well as example agents that describe simple uses of the artifact can be found in the project (src/agt).

### To Start

Run those simple example agent and read their description to understand how works the artifact.
