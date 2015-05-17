$(function(){$('.spinner').hide();});
function fnLogin(){$('.spinner').show();$.post('/pt/token',{},function(data){bootbox.dialog({message:data});$('.spinner').hide();});}
/** reply */
function fnReply(id,screenName){id = id.replace('reply_','');bootbox.dialog({message:$('#content_'+id).html()+"<hr/><div contenteditable class='reply-content' name='replystatus'>@"+screenName+"</a></div></div>",title:"Reply to @"+screenName,buttons:{cancel:{label:"Cancel",className:"btn-default",callback:function(){}},ok:{label:"Reply",className:"btn-info",callback:function(){var contenteditable=document.querySelector('[contenteditable]'),text=contenteditable.textContent;$.post('/pt/replyto',{'statusId':id,'content':text},function(data){fadeMsg(data);});}}}});}
/** retweet */
function fnRT(id){id = id.replace('rt_','');bootbox.dialog({message:$('#content_'+id).html(),title:"Retweet this to your fallowers?",buttons:{cancel:{label:"Cancel",className:"btn-default",callback:function(){}},ok:{label:"Retweet",className:"btn-info",callback:function(){$.post('/pt/rt',{'statusId':id},function(data){fadeMsg(data);});}}}});}
/** favorite */
function fnFavorite(id){id = id.replace('favo_','');$.post('/pt/createFV',{'statusId':id},function(data){fadeMsg(data);});}
/** delete */
function fnDel(id){id = id.replace('delete_','');bootbox.dialog({message:$('#content_'+id).html(),title:"Are you sure you want to delete this Tweet?",buttons:{cancel:{label:"Cancel",className:"btn-default",callback:function(){}},ok:{label:"Delete",className:"btn-info",callback:function(){$.post('/pt/destroy',{'statusId':id},function(data){fadeMsg(data);});}}}});}
/** send msg */
function fnDirectMsg(){bootbox.dialog({message:'To <input id="screenName" class="form-control"><br/><textarea class="form-control" rows="5" id="directMsg"></textarea>',title:'Direct Messages > New',buttons:{cancel:{label:"Cancel",className:"btn-default",callback:function(){}},ok:{label:"Send Message",className:"btn-success",callback:function(){$.post('/pt/newMsg',{'screenName':$('#screenName').val(),'directMsg':$('#directMsg').val(),},function(data){fadeMsg(data);});}}}});}
/** callback msg */
function fadeMsg(text){var elem=$('.bb-alert');if(elem.length){elem.find('span').html(text);}else{elem=$('<div class="bb-alert alert alert-info"><span>'+text+'</span></div>').appendTo('.container');}elem.delay(2000).fadeIn().delay(2000).fadeOut();}