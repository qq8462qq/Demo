/*
//文件上传服务层
app.service("uploadService",function($http){
    this.uploadFile=function(){
        alert(2222)
        var formData=new FormData();
        formData.append("file",file.files[0]);
        return $http({
            method:'POST',
            url:"../upload.do",
            data: formData,
            headers: {'Content-Type':undefined},
            transformRequest: angular.identity
        });
    }
});*/
app.service('uploadService',function($http){

    //上传文件
    this.uploadFile=function(){
        var formdata=new FormData();
        formdata.append('file',file.files[0]);//file 文件上传框的name
        alert(2222)
        return $http({
            url:'../upload.do',
            method:'post',
            data:formdata,
            headers:{ 'Content-Type':undefined },
            transformRequest: angular.identity
        });

    }


});
