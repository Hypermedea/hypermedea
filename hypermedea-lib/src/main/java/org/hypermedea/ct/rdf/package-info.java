/**
 * <p>
 *   RDF statements (triples) map to Jason terms of the form:
 *   <code>rdf(S, P, O)[ rdf_type_map(SType, uri, OType) ]</code>
 * </p>
 * <p>
 *   The mapping from RDF node types to Jason may be ambiguous. URIs and string literals map both
 *   to Jason strings. To disambiguate the two, agents can look up the <code>rdf_type_map</code>
 *   annotation, which gives the node type of the triple's subject, predicate and object (note
 *   that the predicate's node type is always <code>uri</code>). The full mapping is as follows:
 * </p>
 * <table>
 *   <caption>Mapping between RDF and Jason structures</caption>
 *   <tr>
 *       <th>RDF type</th>
 *       <th>Jason type</th>
 *       <th>Value of <code>rdf_type_map</code></th>
 *   </tr>
 *   <tr>
 *       <td>Named resource (URI)</td>
 *       <td>String</td>
 *       <td><code>uri</code></td>
 *   </tr>
 *   <tr>
 *       <td>Blank node</td>
 *       <td>Atom</td>
 *       <td><code>bnode</code></td>
 *   </tr>
 *   <tr>
 *       <td>Literal (number)</td>
 *       <td>Number</td>
 *       <td><code>literal</code></td>
 *   </tr>
 *   <tr>
 *       <td>Literal (any other type)</td>
 *       <td>String</td>
 *       <td><code>literal</code></td>
 *   </tr>
 * </table>
 *
 * <p>
 *     For example, the RDF graph and Jason belief base given below are equivalent:
 * </p><pre>@prefix ex: &lt;http://example.org/&gt; .
ex:alice a ex:Person .
ex:alice ex:name "Alice" .
ex:alice ex:age 42 .
ex:alice ex:knows _:someone .</pre>
   <pre>rdf("http://example.org/alice", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://example.org/Person") [ rdf_type_map(uri, uri, uri) ] .
rdf("http://example.org/alice", "http://example.org/name", "Alice") [ rdf_type_map(uri, uri, literal) ] .
rdf("http://example.org/alice", "http://example.org/age", 42) [ rdf_type_map(uri, uri, literal) ] .
rdf("http://example.org/alice", "http://example.org/knows", someone) [ rdf_type_map(uri, uri, bnode) ] .</pre>
 *
 */
package org.hypermedea.ct.rdf;