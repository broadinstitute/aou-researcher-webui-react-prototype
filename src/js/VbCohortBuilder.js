window.VbCohortBuilder = (function() {
  var self = {};

  self.render = function(domElement) {
    var parent = document.createElement('div');
    var child = document.createTextNode('I build cohorts!');
    parent.appendChild(child);
    domElement.appendChild(parent);
  };

  return self;
})()
