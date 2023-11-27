/**
 * <p>
 *   <b>Main Hypermedea package</b>. Hypermedea is a collection of utilities to develop Web
 *   agents written in the <a href="https://jason-lang.github.io/doc/">Jason</a> language,
 *   opening up to the <a href="http://jacamo-lang.github.io/jacamo/">JaCaMo</a>
 *   multi-agent oriented programming framework.
 * </p>
 * <p>
 *   Hypermedea can be used in two ways:
 * </p>
 * <ul>
 *   <li>
 *     as a Jason environment, acting as an application proxy to the Web.
 *     See {@link org.hypermedea.HypermedeaEnvironment}.
 *   </li>
 *   <li>
 *     as part of programmable environment, i.e. as a CArtAgO artifact.
 *     See {@link org.hypermedea.HypermedeaArtifact}.
 *   </li>
 * </ul>
 * <p>
 *   Both classes expose the same operations to agents: GET, WATCH, PUT, POST, PATCH and DELETE,
 *   although with slight differences (refer to their respective documentation for details on the
 *   operations' signature). These operations are atomic operations to be perform on Web resources,
 *   as per the A+REST architectural style (A for asynchronous): retrieval (GET, WATCH), creation
 *   or extension (POST), replacement (PUT), update (PATCH) and removal (DELETE).
 * </p>
 * <p>
 *   Retrieval operations (GET, WATCH) have the effect that the retrieved representation is stored
 *   locally by the artifact. To delete it, a FORGET operation is also available. FORGET also
 *   allows agents to "unwatch" a resource, i.e. to unsubscribe from server notifications.
 * </p>
 * <p>
 *   Operations are independent of the underlying communication protocol and of the representation
 *   of the exchanged resources (or resource parts). Hypermedea relies on dynamically loaded
 *   <em>protocol bindings</em> and <em>representation handlers</em> (also known as payload bindings):
 * </p>
 * <ul>
 *   <li>
 *     a protocol binding registers itself for a particular URI scheme (e.g. <code>http:</code> or
 *     <code>mqtt:</code>). If an operation targets the <code>http://example.org/some-resource</code>
 *     URI, the operation will be handled by the HTTP binding, which is loaded by default. Hypermedea
 *     also has a default binding for file URIs. See the documentation of the
 *     {@link org.hypermedea.op op} package for more details.
 *   </li>
 *   <li>
 *     a representation handler registers itself for a particular Content-Type (e.g.
 *     <code>application/json</code>, <code>text/turtle</code> or <code>text/plain</code>). It can
 *     consume a Jason structure literal (such as <code>rdf("s", "p", "o")</code> to serialize it
 *     into RDF or deserialize an RDF payload to produce a corresponding Jason structure. See the
 *     documentation of the {@link org.hypermedea.ct ct} package for more details.
 *   </li>
 * </ul>
 * <p>
 *   See <a href="https://doi.org/10.1109/ICSE.2004.1317465">"Extending the Representational
 *   State Transfer (REST) Architectural Style for Decentralized Systems"</a> for more details on A+REST
 *   and <a href="https://w3c.github.io/wot-binding-templates/">Web of Things (WoT) Binding Templates</a>
 *   for more details on the idea of protocol/payload bindings.
 * </p>
 */
package org.hypermedea;