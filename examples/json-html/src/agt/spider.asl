!print_link_targets .

+!print_link_targets
    <-
    get("https://jason-lang.github.io/") ;
    ?(dom(Doc)) ;
    .map.get(Doc, links, Links) ;
        for (.member(Link, Links)) {
        .map.get(Link, attributes, Attrs) ;
        .map.get(Attrs, href, URI) ;
        .print(URI) ;
    } ;
  .