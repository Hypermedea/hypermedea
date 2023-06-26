/**
 * <p>
 *     Wrapper classes for agents to handle JSON values (used by the {@link org.hypermedea.ThingArtifact}).
 * </p>
 * <p>
 *     The mapping between JSON objects and Jason terms is inspired by
 *     <a href="https://www.swi-prolog.org/pldoc/man?section=jsonsupport">the SWI-Prolog JSON library</a>.
 *     Simple JSON values and arrays have an straightforward equivalent in Jason. JSON objects are represented
 *     in Jason as unary structures holding a list of key-value pairs, themselves represented as binary structures.
 *     See examples:
 * </p>
 * <table>
 *     <tr>
 *         <th>JSON</th>
 *         <th>Jason</th>
 *     </tr>
 *     <tr>
 *         <td><code>true</code>, <code>false</code></td>
 *         <td><code>true</code>, <code>false</code></td>
 *     </tr>
 *     <tr>
 *         <td><code>null</code></td>
 *         <td><code>null</code> (encoded as an atom)</td>
 *     </tr>
 *     <tr>
 *         <td><code>42.5</code></td>
 *         <td><code>42.5</code></td>
 *     </tr>
 *     <tr>
 *         <td><code>"abc"</code></td>
 *         <td><code>"abc"</code> or <code>abc</code> (if valid Jason atom)</td>
 *     </tr>
 *     <tr>
 *         <td><code>[ 1, 2, 3, 4 ]</code></td>
 *         <td><code>[ 1, 2, 3, 4 ]</code></td>
 *     </tr>
 *     <tr>
 *         <td><code>{ "a": 12.5, "b": [...] }</code></td>
 *         <td><code>json([kv("a", 12.5), kv("b", [...])])</code> (object keys may also be Jason atoms)</td>
 *     </tr>
 * </table>
 */
package org.hypermedea.json;