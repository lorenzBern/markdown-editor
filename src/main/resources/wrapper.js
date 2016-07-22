/* implementation of ch.webruler.app.markdown.util.Renderer interface */
function render(s) {
    return "<html><body>"+marked(s, {gfm: true})+"</body</html>";
}