function Page(page) {
    globalThis._page = page;
    if (page['data'] == undefined) {
        page.data = {}
    }

    page.setData = function (data) {
        var keys = Object.keys(data);
        for (var i = 0; i < keys.length; i++) {
            var key = keys[i];
            page.data[key] = data[key];
        }
        render.setData(data);
    }
    var methods = {};
    Object.keys(page).forEach(function (key) {
        var obj = page[key];
        if (typeof obj === "function") {
            methods[key] = {}
        }
    });
    render.initRender(page.data, methods);
}
function invokeFunction(name, params) {
    globalThis._page[name](params);
}