/* implementation of candown.Renderer interface */
function render(s) {
    return "<html><body>"+marked(s, {gfm: true})+"</body</html>";
}