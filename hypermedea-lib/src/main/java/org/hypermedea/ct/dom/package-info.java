/**
 * <p>
 *     XML and HTML documents are exposed to Jason agents via the
 *     <a href="https://developer.mozilla.org/en-US/docs/Web/API/Document_Object_Model">Document Object Model (DOM)
 *     API</a>. A document corresponds to a Java Map whose keys are (a subset of) the properties of the DOM
 *     <code>Document</code> class, such as <code>links</code>, <code>forms</code> and <code>documentElement</code>,
 *     which maps to the root element of the document. An element also corresponds to a Java Map whose keys are
 *     properties of the DOM <code>Element</code> class. Elements have child nodes, which can either be text nodes
 *     or elements. Comments are not included. Text nodes that only consist of white space and new line characters
 *     are not included.
 * </p>
 * <p>
 *     The example HTML document below:
 * </p>
 * <pre><code>&lt;html lang="en" class="e"&gt;
  &lt;head>&lt;title>Test page&lt;/title&gt;&lt;/head&gt;
  &lt;body&gt;Hi &lt;a href="https://jason-lang.github.io/"&gt;Jason&lt;/a&gt;!&lt;/body&gt;
&lt;/html&gt;</code></pre>
 * <p>
 *      is represented in Jason as follows:
 * </p>
 * <pre><code>dom({
  links->[
    {
      attributes -> { href -> "https://jason-lang.github.io/" },
      child_nodes -> [ "Jason" ],
      tag -> a
    }
  ],
  forms -> [],
  title -> "Test page",
  document_element -> {
    attributes -> { class -> "e", lang -> "en" },
    child_nodes->[
      {
        attributes -> {},
        child_nodes -> [
          {
            attributes -> {},
            child_nodes -> ["Test page"],
            tag -> title
          }
        ],
        tag -> head
      }, {
        attributes -> {},
        child_nodes -> [
          "Hi ",
          {
            attributes -> { href -> "https://jason-lang.github.io/" },
            child_nodes -> [ "Jason" ],
            tag -> a
          },
          "!"
        ],
        tag -> body
      }
    ],
    tag -> html
  }
})</code></pre>
 */
package org.hypermedea.ct.dom;