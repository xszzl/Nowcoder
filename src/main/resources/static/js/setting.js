$(function () {
    $("#uploadForm").submit(upload);
});

function upload() {
    $.ajax({
        url: "http://upload-z1.qiniup.com",
        method: "post",
        // 不要将文件转成字符串
        processData: false,
        // 让浏览器自己处理
        contentType: false,
        // 数据为表单
        data: new FormData($("#uploadForm")[0]),
        success: function (data) {
            if (data && data.code == 200){
                // 更新头像路径
                $.post(
                    CONTEXT_PATH + "/user/header/url",
                    {"fileName":$("input[name='key']").val()},
                    function (data) {
                        data = $.parseJSON(data);
                        if (data.code == 200){
                            window.location.reload();
                        } else {
                            alert(data.msg);
                        }
                    }
                );
            } else {
                alert("上传失败！");
            }
        }
    });
    return false;
}