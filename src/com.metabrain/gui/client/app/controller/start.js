app.controller("start", function ($scope, $mdDialog) {

    var margin = {top: -5, right: -5, bottom: -5, left: -5},
        width = 700 - margin.left - margin.right,
        height = 500 - margin.top - margin.bottom;

    var zoom = d3.behavior.zoom()
        .scaleExtent([1, 10])
        .on("zoom", zoomed);

    var drag = d3.behavior.drag()
        .origin(function (d) {
            return d;
        })
        .on("dragstart", dragstarted)
        .on("drag", dragged)
        .on("dragend", dragended);

    var startTime;
    var endTime;

    var svg = d3.select("#canvas").append("svg")
        .attr("width", width + margin.left + margin.right)
        .attr("height", height + margin.top + margin.bottom)
        .append("g")
        .attr("transform", "translate(" + margin.left + "," + margin.right + ")");

    var rect = svg.append("rect")
        .attr("width", width)
        .attr("height", height)
        .style("fill", "none")
        .style("pointer-events", "all")
        .call(zoom)
        .on('mousedown', function() {
            startTime = new Date();
            console.log(d3.mouse(this));
        })
        .on('mouseup',function() {
            let pos = d3.mouse(this);
            console.log(pos);
            endTime = new Date();
            if ((endTime - startTime) > 300) {
                console.log("long click, " + (endTime - startTime) + " milliseconds long");
            }
            else {
                console.log("regular click, " + (endTime - startTime) + " milliseconds long");
            }
        });

    var container = svg.append("g");

    container.append("g")
        .attr("class", "x axis")
        .selectAll("line")
        .data(d3.range(0, width, 10))
        .enter().append("line")
        .attr("x1", function (d) {
            return d;
        })
        .attr("y1", 0)
        .attr("x2", function (d) {
            return d;
        })
        .attr("y2", height);

    container.append("g")
        .attr("class", "y axis")
        .selectAll("line")
        .data(d3.range(0, height, 10))
        .enter().append("line")
        .attr("x1", 0)
        .attr("y1", function (d) {
            return d;
        })
        .attr("x2", width)
        .attr("y2", function (d) {
            return d;
        });

    var data = d3.range(20).map(function () {
        return {x: Math.random() * width, y: Math.random() * height};
    });

    dot = container.append("g")
        .attr("class", "dot")
        .selectAll("circle")
        .data(data)
        .enter().append("circle")
        .attr("r", 20)
        .attr("cx", function (d) {
            return d.x;
        })
        .attr("cy", function (d) {
            return d.y;
        })
        .call(drag);

    function zoomed() {
        container.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
    }

    function dragstarted(d) {
        startTime = new Date();
        console.log(d);
        d3.event.sourceEvent.stopPropagation();
        d3.select(this).classed("dragging", true);
    }

    function dragged(d) {
        d3.select(this).attr("cx", d.x = d3.event.x).attr("cy", d.y = d3.event.y);
    }

    function dragended(d) {
        d3.select(this).classed("dragging", false);
    }

    $scope.request('getNode', {
        nodeId: 0
    }, function (data) {

    });

    $scope.openDialog = function (number) {
        $mdDialog.show({
            controller: function ($scope, number) {
                $scope.number = number;
                $scope.answer = function (result) {
                    alert(result);
                }
            },
            templateUrl: 'app/template/start_dialog.html',
            locals: {
                number: number
            }
        })
            .then(function (answer) {
                $scope.status = 'You said the information was "' + answer + '".';
            }, function () {
                $scope.status = 'You cancelled the dialog.';
            });
    }
});