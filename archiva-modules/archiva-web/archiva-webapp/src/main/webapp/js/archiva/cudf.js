/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
define( "archiva.cudf",
        ["jquery", "i18n", "jquery.tmpl", "knockout", "knockout.simpleGrid", "jqueryFileTree", "prettify"],
function ( jquery, i18n, jqueryTmpl, ko, koSimpleGrid ) {

  getCUDFExtract = function ( groupId, artifactId, version ) {
    $( "#artifact-details-cudf #get-cudf-spinner-div" ).html( smallSpinnerImg() );
    $( "<form action='restServices/archivaServices/cudfService/cone/" + groupId + "/" + artifactId + "/" + version
           + "' method='GET' accept='application/octet-stream'>" + "<input type='text' name='repositoryId' value='"
           + getSelectedBrowsingRepository() + "'/>" + "</form>" ).submit();
    removeSmallSpinnerImg( "#artifact-details-cudf #get-cudf-spinner-div" );
  }

  showCUDFExtract = function ( groupId, artifactId, version ) {
    $( "#artifact-details-cudf #get-cudf-spinner-div" ).html( smallSpinnerImg() );
    var metadataUrl = "restServices/archivaServices/cudfService/cone/";
    metadataUrl += groupId + "/";
    metadataUrl += artifactId + "/";
    metadataUrl += version;
    var selectedRepo = getSelectedBrowsingRepository();
    if ( selectedRepo )
    {
      metadataUrl += "?repositoryId=" + encodeURIComponent( selectedRepo );
    }
    $.ajax( metadataUrl, {
      type:"GET",
      dataType:"html",
      success:function ( data ) {
        $( "#show-cudf-content" ).html( data.replace( /\n/g, '<br />' ) );
        $( "#show-cudf-content" ).removeClass( "hide" );
        removeSmallSpinnerImg( "#artifact-details-cudf #get-cudf-spinner-div" );
        $( "#show-cudf-button" ).addClass( "disabled" );
      },
      error:function ( data ) {
        displayErrorMessage( $.i18n.prop( 'cudf.extract.error-message' ) );
        removeSmallSpinnerImg( "#artifact-details-cudf #get-cudf-spinner-div" );
      }
    } );
  }

  displayCUDFUniverse = function () {
    screenChange();
    var mainContent = $( "#main-content" );
    mainContent.html( mediumSpinnerImg() );
    mainContent.html( $( "#cudf_universe_tmpl" ).tmpl() );
  }

  showCUDFUniverseExtract = function () {
    $( "#get-cudf-universe-spinner-div" ).html( smallSpinnerImg() );
    var metadataUrl = "restServices/archivaServices/cudfService/universe";
    var selectedRepo = getSelectedBrowsingRepository();
    if ( selectedRepo )
    {
      metadataUrl += "?repositoryId=" + encodeURIComponent( selectedRepo );
    }
    $.ajax( metadataUrl, {
      type:"GET",
      dataType:"html",
      success:function ( data ) {
        $( "#show-cudf-universe-content" ).html( data.replace( /\n/g, '<br />' ) );
        $( "#show-cudf-universe-content" ).removeClass( "hide" );
        removeSmallSpinnerImg( "#get-cudf-universe-spinner-div" );
        $( "#show-cudf-universe-button" ).addClass( "disabled" );
      },
      error:function ( data ) {
        displayErrorMessage( $.i18n.prop( 'cudf.extract.error-message' ) );
        removeSmallSpinnerImg( "#get-cudf-universe-spinner-div" );
      }
    } );
  }

  getCUDFUniverseExtract = function () {
    $( "<form id='extract-CUDF' style='display: none;' action='restServices/archivaServices/cudfService/universe' method='GET' accept='application/octet-stream'>"
           + "<input type='text' name='repositoryId' value='" + getSelectedBrowsingRepository() + "'/>"
           + "<input type='submit'>" + "</form>" ).appendTo( 'body' );
    $( '#extract-CUDF' ).submit();
    $( '#extract-CUDF' ).remove();
  }

  CUDFJob = function ( id, location, cronExpression, allRepositories, repositoryGroup, debug ) {
    var self = this;
    this.id = ko.observable( id );
    this.location = ko.observable( location );
    this.cronExpression = ko.observable( cronExpression );
    this.allRepositories = ko.observable( allRepositories );
    this.repositoryGroup = ko.observable( repositoryGroup );
    this.debug = ko.observable( debug );
  }

  CUDFJobViewModel = function ( cudfJob, cudfJobsViewModel, update ) {
    this.cudfJob = cudfJob;
    this.cudfJobsViewModel = cudfJobsViewModel;
    this.update = update;

    var self = this;

    save = function () {
      var valid = $( "#main-content #cudf-job-edit-form" ).valid();
      if ( valid == false )
      {
        return;
      }
      $.log( "save: CUDF job" );
      clearUserMessages();
      if ( update )
      {
        $.ajax( "restServices/archivaServices/cudfService/jobs/" + this.cudfJob.id(), {
          type:"PUT",
          data:ko.toJSON( this.cudfJob ),
          contentType:'application/json',
          dataType:'json',
          success:function ( data ) {
            displaySuccessMessage( $.i18n.prop( 'cudf.job.message.success' ) );
            activateCUDFJobGridTab();
          },
          error:function ( data ) {
            var res = $.parseJSON( data.responseText );
            displayRestError( res );
          }
        } );
      }
      else
      {
        $.ajax( "restServices/archivaServices/cudfService/jobs/", {
          type:"POST",
          data:ko.toJSON( this.cudfJob ),
          contentType:'application/json',
          dataType:'json',
          success:function ( data ) {
            self.cudfJobsViewModel.cudfJobs.push( cudfJob );
            displaySuccessMessage( $.i18n.prop( 'cudf.job.message.success' ) );
            activateCUDFJobGridTab();
          },
          error:function ( data ) {
            var res = $.parseJSON( data.responseText );
            displayRestError( res );
          }
        } );
      }
    };

    reset = function () {
      displayCUDFJobs();
    };
  }

  CUDFJobsViewModel = function () {
    var self = this;
    this.cudfJobs = ko.observableArray( [] );
    this.availableRepositoryGroups = ko.observableArray( [] );

    this.cudfJobsViewModel = new ko.simpleGrid.viewModel(
      {
        data:this.cudfJobs,
        viewModel:this,
        columns:[
          {headerText:$.i18n.prop( 'cudf.job.table.header.id' ), rowText:"id"},
          {headerText:$.i18n.prop( 'cudf.job.table.header.cronExpression' ), rowText:"cronExpression"},
          {headerText:$.i18n.prop( 'cudf.job.table.header.repositoryGroup' ), rowText:"repositoryGroup"}
        ],
        pageSize:10
      }
    );

    editCUDFJob = function ( cudfJob ) {
      loadAvailableRepositoryGroups( function ( data ) {
        var cudfJobViewModel = new CUDFJobViewModel( cudfJob, self, true );
        activateCUDFJobEditTab();
        ko.applyBindings( cudfJobViewModel, $( "#main-content #cudf-jobs-edit" ).get( 0 ) );
        $( "#main-content #cudf-jobs-view-tabs-li-edit a" ).html( $.i18n.prop( "cudf.job.tab.edit.title" ) );
        activateCUDFJobFormValidation();
        activateCUDFRepositoryGroupsCollapse( cudfJob );
      } );
    }

    deleteCUDFJob = function ( cudfJob ) {
      openDialogConfirm( function () {
                           self.removeCUDFJob( cudfJob );
                           window.modalConfirmDialog.modal( "hide" );
                         }, $.i18n.prop( "ok" ), $.i18n.prop( "cancel" ),
                         $.i18n.prop( "cudf.job.message.delete.confirm", cudfJob.id() ),
                         $( "#cudf-job-delete-warning-tmpl" ).tmpl( self.cudfJob ) );
    }

    this.removeCUDFJob = function ( cudfJob ) {
      clearUserMessages();
      $.ajax( "restServices/archivaServices/cudfService/jobs", {
        type:"DELETE",
        data:ko.toJSON( cudfJob ),
        contentType:"application/json",
        dataType:'json',
        success:function ( data ) {
          var message = $.i18n.prop( "cudf.job.message.delete", cudfJob.id() );
          displaySuccessMessage( message );
          self.cudfJobs.remove( cudfJob );
        },
        error:function ( data ) {
          var res = $.parseJSON( data.responseText );
          displayRestError( res );
        }
      } );
    }

    launchCUDFJob = function ( cudfJob ) {
      openDialogConfirm( function () {
                           closeDialogConfirm();
                           $.ajax( "restServices/archivaServices/cudfService/jobs/" + cudfJob.id() + "/start", {
                             type:'POST',
                             success:function () {
                               displaySuccessMessage( $.i18n.prop( "cudf.job.launch.successful", cudfJob.id() ) );
                             },
                             error:function () {
                               displayErrorMessage( $.i18n.prop( "cudf.job.launch.failure", cudfJob.id() ) );
                             }
                           } );
                         }, $.i18n.prop( "cudf.job.launch.ok" ), $.i18n.prop( "cudf.job.launch.cancel" ),
                         $.i18n.prop( "cudf.job.launch.title", cudfJob.id() ),
                         $.i18n.prop( "cudf.job.launch.text", cudfJob.id() ) );
    }
  }

  displayCUDFJobs = function () {
    screenChange();
    var mainContent = $( "#main-content" );
    mainContent.html( mediumSpinnerImg() );

    var self = this;
    this.cudfJobsViewModel = new CUDFJobsViewModel();

    loadCUDFJobs( function ( data ) {
      self.cudfJobsViewModel.cudfJobs( mapCUDFJobs( data ) );
      mainContent.html( $( "#cudfJobsMain" ).tmpl() );
      ko.applyBindings( self.cudfJobsViewModel, mainContent.find( "#cudf-jobs-view" ).get( 0 ) );

      loadAvailableRepositoryGroups( function ( data ) {
        self.cudfJobsViewModel.availableRepositoryGroups( mapAvailableRepositoryGroups( data ) );
      } );

      mainContent.find( "#cudf-jobs-view-tabs" ).on( "show", function ( e ) {
        if ( $( e.target ).attr( "href" ) == "#cudf-jobs-edit" )
        {
          var cudfJob = new CUDFJob();
          cudfJob.allRepositories( true );
          var cudfJobViewModel = new CUDFJobViewModel( cudfJob, self.cudfJobsViewModel, false );
          activateCUDFJobEditTab();
          ko.applyBindings( cudfJobViewModel, mainContent.find( "#cudf-jobs-edit" ).get( 0 ) );
          activateCUDFJobFormValidation();
        }
        if ( $( e.target ).attr( "href" ) == "#cudf-jobs-view" )
        {
          mainContent.find( "#cudf-jobs-view-tabs-li-edit a" ).html( $.i18n.prop( "cudf.job.tab.add.title" ) );
          clearUserMessages();
        }
      } )
    }, function () {
      var res = $.parseJSON( data.responseText );
      displayRestError( res );
    } );
  }

  mapCUDFJobs = function ( data ) {
    if ( data != null )
    {
      return $.isArray( data ) ? $.map( data, function ( item ) {
        return mapCUDFJob( item );
      } ) : [mapCUDFJob( data )];
    }
    return [];
  }

  mapCUDFJob = function ( data ) {
    return data == null ? null : new CUDFJob( data.id, data.location, data.cronExpression, data.allRepositories,
                                              data.repositoryGroup === null ? "" : data.repositoryGroup, data.debug );
  }

  mapAvailableRepositoryGroups = function ( data ) {
    if ( data == null )
    {
      return null;
    }
    var availableRepositoryGroups = [];
    $.each( data, function ( index, value ) {
      availableRepositoryGroups.push( value.id );
    } );
    return availableRepositoryGroups;
  }

  activateCUDFJobEditTab = function () {
    var mainContent = $( "#main-content" );

    mainContent.find( "#cudf-jobs-view-tabs-content div[class*='tab-pane']" ).removeClass( "active" );
    mainContent.find( "#cudf-jobs-view-tabs li" ).removeClass( "active" );

    mainContent.find( "#cudf-jobs-edit" ).addClass( "active" );
    mainContent.find( "#cudf-jobs-view-tabs-li-edit" ).addClass( "active" );
  }

  activateCUDFJobGridTab = function () {
    var mainContent = $( "#main-content" );

    mainContent.find( "#cudf-jobs-edit" ).removeClass( "active" );
    mainContent.find( "#cudf-jobs-view-tabs li" ).removeClass( "active" );

    mainContent.find( "#cudf-jobs-view" ).addClass( "active" );
    mainContent.find( "#cudf-jobs-view-tabs-li-grid" ).addClass( "active" );
    mainContent.find( "#cudf-jobs-view-tabs-li-edit a" ).html( $.i18n.prop( "cudf.job.tab.add.title" ) );
  }

  activateCUDFJobFormValidation = function () {
    var validator = $( "#main-content #cudf-job-edit-form" ).validate(
        {
          rules:{
            location:{
              required:true
            },
            cronExpression:{
              required:true,
              remote:{
                url:"restServices/archivaServices/commonServices/validateCronExpression",
                type:"get"
              }
            }
          },
          showErrors:function ( validator, errorMap, errorList ) {
            customShowError( "#main-content #cudf-job-edit-form", validator, errorMap, errorMap );
          }
        } );
    validator.settings.messages["cronExpression"] = $.i18n.prop( "cudf.job.message.validation.cronExpression" );
    validator.settings.messages["location"] = $.i18n.prop( "cudf.job.message.validation.location" );
  }

  activateCUDFRepositoryGroupsCollapse = function ( cudfJob ) {
    if ( !cudfJob.allRepositories() )
    {
      $( "#cudf-job-repository-groups-list" ).collapse();
    }
  }

  loadAvailableRepositoryGroups = function ( successCallBackFn, errorCallBackFn ) {
    $.ajax( "restServices/archivaServices/repositoryGroupService/getRepositoriesGroups", {
      type:"GET",
      dataType:"json",
      success:successCallBackFn,
      error:errorCallBackFn
    } );
  }

  loadCUDFJobs = function ( successCallBackFn, errorCallBackFn ) {
    $.ajax( "restServices/archivaServices/cudfService/jobs", {
      type:"GET",
      dataType:"json",
      success:successCallBackFn,
      error:errorCallBackFn
    } )
  }
})