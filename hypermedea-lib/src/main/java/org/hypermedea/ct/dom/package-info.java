/**
 * <p>
 *     XML and HTML documents are exposed to Jason agents via the
 *     <a href="https://developer.mozilla.org/en-US/docs/Web/API/Document_Object_Model">Document Object Model (DOM)
 *     API</a>. A document corresponds to a Java Map whose keys are (a subset of) the properties of the DOM
 *     <code>Document</code> class, such as <code>links</code>, <code>forms</code> and <code>rootElement</code>,
 *     which maps to a DOM element. An element also corresponds to a Java Map whose keys are properties of the DOM
 *     <code>Element</code> class, and so on for child objects. The methods of DOM objects are not exposed.
 * </p>
 * <p>
 *     For instance, if a representation is exposed as <code>dom(Doc)</code>, <code>Doc</code> should be a Java Map
 *     that exposes properties of the document and all elements it contains.
 * </p>
 */
package org.hypermedea.ct.dom;