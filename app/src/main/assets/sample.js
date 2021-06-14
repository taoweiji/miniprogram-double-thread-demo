// function Page(obj) {
//     self.page = obj;
// }
// Page({
//     data: {
//         message: 'Hello MINA!'
//     },
//     onLoad: function (options) {
//         // 页面创建时执行
//         this.data.message = ""
//     },
//     onShow: function () {
//         // 页面出现在前台时执行
//     },
// })
var count = 0;
function timer() {
    setData({
        message: count++
    })
    setTimeout(timer, 1000)
}
timer();