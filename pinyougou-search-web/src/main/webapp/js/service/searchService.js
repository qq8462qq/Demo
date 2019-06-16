app.service('searchService',function ($http) {
    this.search=function (searchMap) {
        return $http.post('itrmsearch/search.do',searchMap);
    }


})