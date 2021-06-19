Page({
    data: {
        counter: 1024
    },

    onMinusClick: function () {
        this.setData({ counter: this.data.counter - 1 })
    },
    onAddClick: function () {
        this.setData({ counter: this.data.counter + 1 })
    }
});