var app = angular.module('myapp', []);

app.controller('TestController', function($scope) {
    $scope.items = [
        {'name': 'item1',
         'snippet': 'the 1st iten'},
        {'name': 'item2',
         'snippet': 'the 2nd item'},
        {'name': 'item3',
         'snippet': 'the 3rd item'}
    ]
});
