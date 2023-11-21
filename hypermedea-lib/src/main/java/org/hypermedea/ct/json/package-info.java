/**
 * <p>
 *     The mapping between JSON objects and Jason terms is inspired by
 *     <a href="https://www.swi-prolog.org/pldoc/man?section=jsonsupport">the SWI-Prolog JSON library</a>.
 *     Simple JSON values and arrays have a straightforward equivalent in Jason. JSON objects are represented
 *     in Jason as unary structures holding a list of key-value pairs, themselves represented as binary structures.
 *     See examples:
 * </p>
 * <table>
 *     <caption>Mapping between JSON and Jason structures</caption>
 *     <tr>
 *         <th>JSON</th>
 *         <th>Jason</th>
 *         <th>Note</th>
 *     </tr>
 *     <tr>
 *         <td><code>true</code>, <code>false</code></td>
 *         <td><code>true</code>, <code>false</code></td>
 *     </tr>
 *     <tr>
 *         <td><code>null</code></td>
 *         <td><code>null</code></td>
 *         <td>(encoded as a Jason atom)</td>
 *     </tr>
 *     <tr>
 *         <td><code>42.5</code></td>
 *         <td><code>42.5</code></td>
 *     </tr>
 *     <tr>
 *         <td><code>"abc"</code></td>
 *         <td><code>"abc"</code> or <code>abc</code></td>
 *         <td>(if valid Jason atom)</td>
 *     </tr>
 *     <tr>
 *         <td><code>[ 1, 2, 3, 4 ]</code></td>
 *         <td><code>[ 1, 2, 3, 4 ]</code></td>
 *     </tr>
 *     <tr>
 *         <td><code>{ "a": 12.5, "b": [...] }</code></td>
 *         <td><code>[kv("a", 12.5), kv("b", [...])]</code></td>
 *         <td>(object keys may also be Jason atoms)</td>
 *     </tr>
 * </table>
 *
 * <p>
 *     Any root JSON value must be enclosed in a <code>json</code> structure.
 *     For instance, the primitive value <code>"abc"</code> or the object <code>[kv("a", 12.5), kv("b", [...])]</code>
 *     are represented respectively as <code>json("abc")</code> and <code>json([kv("a", 12.5), kv("b", [...])])</code>.
 * </p>
 */
package org.hypermedea.ct.json;