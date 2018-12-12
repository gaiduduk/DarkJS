app.controller("main", function ($scope, $mdDialog) {


    let width = 700,
        height = 500;

    var nodeRadius = 20;

    let startTime;

    let currentLink = N + 0;

    let zoom = d3.behavior.zoom()
        .on("zoom", zoomed);

    let svg = d3.select("#canvas")
        .on("touchstart", nozoom)
        .on("touchmove", nozoom)
        .append("svg")
        .attr("width", width)
        .attr("height", height);

    let root = svg.append("g")
        .call(zoom);

    root.append("rect")
        .attr("width", width)
        .attr("height", height)
        .attr("fill", "white")
        .on("mousedown", function () {
            startTime = new Date();
            hideMenu();
        })
        .on("click", function clicked(d, i) {
            if (d3.event.defaultPrevented) return; // zoomed

            if (new Date() - startTime > 300) {
                let pos = d3.mouse(view.node());
                createLocalNode(currentLink, function (link) {
                    setStyle(link, {
                        x: pos[0],
                        y: pos[1],
                        r: nodeRadius,
                    }, function () {
                        showNode(currentLink)
                    })
                });
            }
            d3.select(this).transition()
                .style("fill", "black")
                .transition()
                .style("fill", "white");
        });


    /*
            .on("click", arcTween(outerRadius, 0))
            .on("mouseout", arcTween(outerRadius - 20, 150));*/


    let view = root.append("g")
        .attr("class", "circles");


    var resizeBtn;

    function initResizeBtn() {
        resizeBtn = view.append("circle")
            .attr("class", "resize")
            .style("opacity", 0)
            .attr("r", nodeRadius)
    }

    initResizeBtn();

    function showMenu(ths) {
        let circle = d3.transform(d3.select(ths)).translate;
        resizeBtn.attr("transform", tr(circle[0], circle[1] - nodeRadius * 2 - 10))
            .transition()
            .duration(300)
            .style("opacity", 1);
    }

    function hideMenu() {
        if (resizeBtn.attr("opacity") !== 0)
            resizeBtn.transition()
                .duration(300)
                .style("opacity", 0)
    }

    function zoomed() {
        view.attr("transform", tr(d3.event.translate.x, d3.event.translate.y, d3.event.scale));
    }

    function nozoom() {
        d3.event.preventDefault();
    }


    let drag = d3.behavior.drag()
        .origin(function (d) {
            return d;
        })
        .on("dragstart", dragstarted)
        .on("drag", dragged)
        .on("dragend", dragended);

    let centerOffset;

    function getX(ths) {
        return d3.transform(ths.attr("transform")).translate[0];
    }

    function getY(ths) {
        return d3.transform(ths.attr("transform")).translate[1];
    }

    function dragstarted(d) {
        hideMenu();
        startTime = new Date();
        d3.event.sourceEvent.stopPropagation();
        let clickPos = d3.mouse(this);
        let circle = d3.select(this);
        centerOffset = [getX(circle) - clickPos[0], getY(circle) - clickPos[1]];
        circle.classed("dragging", true);
    }

    function tr(x, y, s) {
        return "translate(" + x + "," + y + ")" + (s === undefined ? "" : "scale(" + s + ")");
    }

    function tr(x, y, s) {
        if (typeof x === "object")
            return "translate(" + x + ")" + (y === undefined ? "" : "scale(" + y + ")");
        return "translate(" + x + "," + y + ")" + (s === undefined ? "" : "scale(" + s + ")");
    }

    function dragged() {
        d3.select(this).attr("transform", tr(d3.mouse(this)));
    }

    function dragended(link) {
        let circle = d3.select(this);
        circle.classed("dragging", false);
        if (new Date() - startTime > 300) {//long click{
            showMenu(this);
            // d3.event.stopPropagation();
        }

        let pos = d3.mouse(this);
        setStyle(link, {
            x: pos[0] + centerOffset[0],
            y: pos[1] + centerOffset[1],
        }, function () {
            showNode(currentLink)
        });
    }

    function showNode(link) {
        currentLink = link;
        let showNode = nodes[currentLink];
        view.selectAll(".node").remove();
        let nodeList = view.selectAll(".node")
            .data(showNode.local || []).enter()
            .append("g")
            .attr("class", "node")
            .attr("transform", function (link) {
                return tr(getStyleValue(link, "x", 0), getStyleValue(link, "y", 0));
            })
            .on("dblclick", function (link) {
                d3.event.stopPropagation();
                openDialog(link)
            })
            .call(drag);

        nodeList.append("circle")
            .attr("r", function (link) {
                return getStyleValue(link, "r", 20);
            });
        nodeList.append("text")
            .attr("dx", nodeRadius + 10)
            .text(showNode.title);
    }


    loadNode(currentLink, function (link) {
        showNode(link);
    });


    function rad(val) {
        return val * (Math.PI / 180)
    }

    function toolbarAnimation() {
        var r = 10000;
        var x = width / 2;
        var y = r + 40;

        var arc = d3.svg.arc()
            .innerRadius(r)
            .outerRadius(r + 1000)
            .startAngle(rad(0))
            .endAngle(rad(360));

        var titleArc = root.append("path")
            .attr("d", arc)
            .attr("transform", tr(x, y))
            .on("click", function () {
                x = width / 2;
                y = height / 2;
                arc.innerRadius(0.01);
                arc.outerRadius(nodeRadius);
                titleArc.transition()
                    .duration(1000)
                    .attr("d", arc)
                    .attr("transform", tr(x, y));
            });
    }

    toolbarAnimation();

    let openDialog = function (link) {

        $mdDialog.show({
            controller: function ($scope, link) {
                $scope.source_code =
                    "function showNode(link) {\n" +
                    "    currentLink = link;\n" +
                    "    let showNode = nodes[currentLink];\n" +
                    "    let circles = view.selectAll(\"circle\")\n" +
                    "        .data(showNode.local || []);\n" +
                    "    circles.exit().remove();\n" +
                    "    circles.enter().append(\"circle\");\n" +
                    "    circles\n" +
                    "        .attr(\"r\", function (link) {\n" +
                    "            return getStyleValue(link, \"r\", 20);\n" +
                    "        })\n" +
                    "        .attr(\"cx\", function (link) {\n" +
                    "            return getStyleValue(link, \"x\", 0);\n" +
                    "        })\n" +
                    "        .attr(\"cy\", function (link) {\n" +
                    "            return getStyleValue(link, \"y\", 0);\n" +
                    "        })\n" +
                    "        .on(\"dblclick\", function (link) {\n" +
                    "            d3.event.stopPropagation();\n" +
                    "            openDialog(link)\n" +
                    "        })\n" +
                    "        .call(drag);\n" +
                    "}\n";

                $scope.link = link;
                $scope.close = function () {
                    $mdDialog.hide();
                };
                $scope.onload = function () {
                    setTimeout(function () {
                        var codeEditor = CodeMirror.fromTextArea(document.getElementById("code"), {
                            styleActiveLine: true,
                            matchBrackets: true,
                            lineNumbers: true,
                            scrollbarStyle: "simple",
                            theme: "darcula"
                        });
                        var runEditor = CodeMirror.fromTextArea(document.getElementById("run_code"), {
                            matchBrackets: true,
                            scrollbarStyle: "simple",
                            theme: "darcula"
                        });
                        var show = setInterval(function () {
                            codeEditor.refresh();
                            runEditor.refresh();
                        }, 10);
                        setTimeout(function () {
                            clearInterval(show);
                        }, 500);
                    });
                };
            },
            templateUrl: 'app/template/main_dialog.html',
            locals: {
                link: link
            },
            clickOutsideToClose: true,
            fullscreen: true,
        }).then(function (answer) {
            $scope.status = 'You said the information was "' + answer + '".';
        }, function () {
            $scope.status = 'You cancelled the dialog.';
        });

    }
});