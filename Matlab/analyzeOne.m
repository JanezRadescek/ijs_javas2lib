clear
javarmpath('C:\Users\janez\workspace\S2_rw\submodules\s2-java-lib\bin')
javarmpath('C:\Users\janez\workspace\S2_rw\submodules\pcardtimesync\bin')
javarmpath('C:\Users\janez\workspace\S2_rw\bin\')

javaaddpath('C:\Users\janez\workspace\S2_rw\submodules\s2-java-lib\bin')
javaaddpath('C:\Users\janez\workspace\S2_rw\submodules\pcardtimesync\bin')
javaaddpath('C:\Users\janez\workspace\S2_rw\bin\')
 
simpleS = javaObject('filters.Runner');
S1 = javaObject('e6.ECG.time_sync.Signal');    
S2 = javaObject('e6.ECG.time_sync.Signal');  

close('all') 

%% Initial input parameters
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% CONSTANTS
nanos_in_one_hour = 3600e9;
colours = 'rgbrmkc';

folder = 'C:\Users\janez\workspace\S2_rw\S2files'
name = 'andrej1.s2'
  


  
%%%%%%%%%%%%%%%%%%%%%%   WORK CONTROL       %%%%%%%%%%%%%%55555
F1original = 0

F1janez = 1;
F2janez = 1;
F1andrej = 1;
F2andrej = 1;

ajanez = 10*10^9;
aandrej = 47589*10^9;

translacija = 100*10^9;
dolzina = 3000*10^9; %% sekunde

	 
	 
	 
	 
	 
	 
%%%%%%%%%%%%%%%%%%%%%%   OLD TIMESTAMPS       %%%%%%%%%%%%%%55555
if F1original
	simpleS.setOldTVP(name);
	stime1 = simpleS.getSamplesTimeStamp();
	stime1 = stime1;% - stime1(1);
	svolt1 = simpleS.getVoltage();
	speaks1 = simpleS.getPeaks();
	ptime1 = simpleS.getPacketsTimeStamp();
	ptime1 = ptime1 - ptime1(1);
	pcounter1 = simpleS.getPacketsCounter();
	%%ping1 = ptime1(2:end) - ptime1(1:end-1);
	%%jitter1 = abs(ping1(2:end) - ping1(1:end-1));
end    



%%%%%%%%%%%%%%%%%%5       NEW TIMESTAMPS         %%%%%%%%%%%%%%%%%%%%%%%%%%%
if F1janez
	simpleS.setNewTVP(name);
	stime2 = simpleS.getSamplesTimeStamp();
	stime2 = stime2;% .- stime2(1);
	svolt2 = simpleS.getVoltage();
	speaks2 = simpleS.getPeaks() + 1;
	ptime2 = simpleS.getPacketsTimeStamp();
	ptime2 = ptime2 .- ptime2(1);
	pcounter2 = simpleS.getPacketsCounter();  
end


%%%%%%%%%%%%%%%%%         ANDREJ TIMESTAMPS  1         %%%%%%%%%%%%%%%%%%%%%%
if F1andrej
	int_length = 3;

	S1.setIntervalLength(int_length);
	S1.readS2File(folder, name, 0, 0.5 * nanos_in_one_hour, 0);
	S1.processSignal;    

	%#  WARNING andrej meèe not dodatne sample

	stime3 = S1.getSamplesTimeStamp; 
	stime3 = stime3;% .- stime3(1);
	svolt3 = S1.getVoltage;
	speaks3 = S1.getPeaks + 1; % +1, because Matlab counting starts from 1
	ptime3 = S1.getNewTimeStamp;  
	ptime3 = ptime3;% .- ptime3(1);
	pcounter3 = S1.getCounter;
end  


  
%%%%%%%%%%%%%%%%%         SECOND FILE           %%%%%%%%%%%%%%%%%%%%%%
if F2janez
	simpleS.setNewTVP("andrej2.s2");
	stime4 = simpleS.getSamplesTimeStamp();
	stime4 = stime4;% .- stime4(1);
	svolt4 = simpleS.getVoltage();
	speaks4 = simpleS.getPeaks() + 1;
	ptime4 = simpleS.getPacketsTimeStamp();
	ptime4 = ptime4 .- ptime4(1);
	pcounter4 = simpleS.getPacketsCounter();  
end





%%%%%%%%%%%%%%%%%         ANDREJ TIMESTAMPS   2          %%%%%%%%%%%%%%%%%%%%%%
if F2andrej
	int_length = 3;

	S2.setIntervalLength(int_length);
	S2.readS2File(folder, 'andrej2.s2', 0, 0.5 * nanos_in_one_hour, 0);
	S2.processSignal;    

	%#  WARNING andrej meèe not dodatne sample

	stime5 = S2.getSamplesTimeStamp; 
	stime5 = stime5;% .- stime5(1);
	svolt5 = S2.getVoltage;
	speaks5 = S2.getPeaks + 1; % +1, because Matlab counting starts from 1
	ptime5 = S2.getNewTimeStamp;  
	ptime5 = ptime5;% .- ptime5(1);
	pcounter5 = S2.getCounter;
end  

 
  


%%%%%%%%%%%%%%%           calculate PING/JITTER         %%%%%%%%%%%%%%%%%
if F1janez && F1andrej && 0 
	ping2 = zeros(size(ptime2)-1,1);
	for i =2:size(ptime2);
		 if (pcounter2(i) - pcounter2(i-1) == 14)
			  ping2(i-1) = ptime2(i) - ptime2(i-1); 
		 end  
	end
	jitter2 = zeros(size(ptime2)-2,1);
	for i =2:size(ping2);
		 if ((pcounter2(i+1) - pcounter2(i) == 14) && (pcounter2(i) - pcounter2(i-1) == 14))
			  jitter2(i-1) = ping2(i) - ping2(i-1); 
		 end  
	end  


	ping3 = zeros(size(ptime3)-1,1);
	for i =2:size(ptime3);
		 if (pcounter3(i) - pcounter3(i-1) == 14)
			  ping3(i-1) = ptime3(i) - ptime3(i-1); 
		 end  
	end
	jitter3 = zeros(size(ptime3)-2,1);
	for i =2:size(ping3);
		 if ((pcounter3(i+1) - pcounter3(i) == 14) && (pcounter3(i) - pcounter3(i-1) == 14))
			  jitter3(i-1) = ping3(i) - ping3(i-1); 
		 end  
	end  
end  

  
  
  
% PRIMERJAVA vrhov med andrej1.s2 in andrej2.s2
% andrej1 je "pravi" andrej2 pa ocenjujemo glede na podobnost s pravim andrej1

%47589 - andrejeva metoda zaèetek



if 1
	if F1janez && F2janez
		a = ajanez + translacija;
		b = a + dolzina;
		tempP2 = speaks2(stime2(speaks2) > a & stime2(speaks2) < b);
		tempP4 = speaks4(stime4(speaks4) > a & stime4(speaks4) < b);
		p = 1;
		r = 1;
		najdeni1 = [];
		while (p<=size(tempP2)(1) && r <= size(tempP4)(1))
			aa = stime4(tempP4(r)) - stime2(tempP2(p));
			if abs(aa) < 2*10^7
		
				
				%%
				
				alpha4 = svolt4(tempP4(r)-1:tempP4(r)+1);
				p4 = 1/2 * (alpha4(1) - alpha4(3)) / (alpha4(1) - 2* alpha4(2) + alpha4(3));
				t4 = stime4(tempP4(r)-1) + (stime4(tempP4(r)) - stime4(tempP4(r)-1)) * (1 + p4);
				
				alpha2 = svolt2(tempP2(p)-1:tempP2(p)+1);
				p2 = 1/2 * (alpha2(1) - alpha2(3)) / (alpha2(1) - 2* alpha2(2) + alpha2(3));
				t2 = stime2(tempP2(p)-1) + (stime2(tempP2(p)) - stime2(tempP2(p)-1)) * (1 + p2);
				
				
				%%
				najdeni1 = [najdeni1, [aa;stime2(tempP2(p));t4-t2]];
				p=p+1;
				r=r+1;
			else
				if aa>0
					p=p+1;
				else
					r=r+1;
				end
				
			end
		end
		
		
		
	end
	%%primerjava vrhov andrejeve metode
	if F1andrej && F2andrej
		a = aandrej + translacija;
		b = a + dolzina;
		tempP3 = speaks3(stime3(speaks3) > a & stime3(speaks3) < b);
		tempP5 = speaks5(stime5(speaks5) > a & stime5(speaks5) < b);
		p = 1;
		r = 1;
		najdeni2 = [];
		while (p<=size(tempP3)(1) & r <= size(tempP5)(1))
			aa = stime5(tempP5(r)) - stime3(tempP3(p));
			if abs(aa) < 2*10^8
				najdeni2 = [najdeni2, [aa;stime3(tempP3(p))]];
				p=p+1;
				r=r+1;
			else
				if aa>0
					p=p+1;
				else
					r=r+1;
				end	
			end
		end
	end	
end
  
  
  
  
%%%%%%%%%%%%%%%%%%%%%%%%%         PLOT             %%%%%%%%%%%%%%%%%%%%%%%%%%  
  


%%%%  PRMERJAVA VRHOV

if F1janez && F2janez && 0
	figure;
	hold on;
	plot(najdeni1(2,:),najdeni1(1,:),'*')
end	
  
%%%%  NAPETOS

if F1janez && 0
	figure;
	hold on; 
	a = ajanez + translacija;
	b = a + dolzina;
	plot(stime2(stime2>a & stime2<b),svolt2(stime2>a & stime2<b),'-g+')
end

if 1
	figure;
	hold on; 
	    
	  %riši nad èasom
	if 1
		vz = 0.3;  %%za lepši izris
		%% 1
		handles = [];
		legends = {};
		
		
		if F1original && 1
		a = ajanez + translacija;
		b = a + dolzina;	
		h=plot(stime1(stime1>a & stime1<b),svolt1(stime1>a & stime1<b),'-r');
		handles = [handles, h];
		legends = {legends{:}, 'andrej1.s2 ne spremenjen'};
		BB = stime1(speaks1);
		CC = BB(BB>a & BB<b);
		DD = svolt1(speaks1);
		EE = DD(BB>a & BB<b);
		plot(CC,EE,'r*','DisplayName','original')
		end
		%% 2	
		if F1janez && 0
		a = ajanez + translacija;
		b = a + dolzina;	
		h=plot(stime2(stime2>a & stime2<b),svolt2(stime2>a & stime2<b)+vz,'-g');
		handles = [handles, h];
		legends = {legends{:}, 'andrej1.s2 janez metoda'};
		BB = stime2(speaks2);
		CC = BB(BB>a & BB<b);
		DD = svolt2(speaks2);
		EE = DD(BB>a & BB<b);
		plot(CC,EE+2*vz,'g*')
		end
		
		
		%% 3
		if F1andrej && 0
		a = aandrej + translacija;
		b = a + dolzina;
		h=plot(stime3(stime3>a & stime3<b),svolt3(stime3>a & stime3<b)./150,'-b');
		handles = [handles, h];
		legends = {legends{:}, 'andrej1.s2 andrej metoda'};
		BB = stime3(speaks3);
		CC = BB(BB>a & BB<b);
		DD = svolt3(speaks3);
		EE = DD(BB>a & BB<b);
		plot(CC,EE./150+2*vz,'b*')
		end
		%% 4
		if F2janez && 0
		a = ajanez + translacija;
		b = a + dolzina;	
		h=plot(stime4(stime4>a & stime4<b),svolt4(stime4>a & stime4<b)-vz,'-m');
		handles = [handles, h];
		legends = {legends{:}, 'andrej2.s2 janez metoda'};
		BB = stime4(speaks4);
		CC = BB(BB>a & BB<b);
		DD = svolt4(speaks4);
		EE = DD(BB>a & BB<b);
		plot(CC,EE+2*vz,'m*')
		end
		
		if F2andrej && 0
		a = aandrej + translacija;
		b = a + dolzina;
		h=plot(stime5(stime5>a & stime5<b),svolt5(stime5>a & stime5<b)./150,'-y');
		handles = [handles, h];
		legends = {legends{:}, 'andrej2.s2 andrej metoda'};
		BB = stime5(speaks5);
		CC = BB(BB>a & BB<b);
		DD = svolt5(speaks5);
		EE = DD(BB>a & BB<b);
		h=plot(CC,EE./150+2*vz,'y*')
		end
		
		if F1janez && F2janez && 1
			a = ajanez + translacija;
			b = a + dolzina;
			h=plot(najdeni1(2,:),najdeni1(1,:)/10^6,'*');
			handles = [handles, h];
			legends = {legends{:}, 'primerjava s2 - janez metoda'};
			vsotaJ = sum(abs(najdeni1(1,:)))
			
			h=plot(najdeni1(2,:),najdeni1(3,:)/10^6,'*');
			handles = [handles, h];
			legends = {legends{:}, 'primerjava s2 z interpolacijo - janez metoda'};
			vsotaJinter = sum(abs(najdeni1(3,abs(najdeni1(3,:))<20*10^6)))
			vsiVrhi = length(najdeni1)
			slabiVrhi = length(najdeni1(3,abs(najdeni1(3,:))>20*10^6))
		end
		
		if F1andrej && F2andrej && 1
			a = aandrej + translacija;
			b = a + dolzina;
			h=plot(najdeni2(2,:)-najdeni2(2,1),najdeni2(1,:)/10^6,'*');
			handles = [handles, h];
			legends = {legends{:}, 'primerjava s2 - andrej metoda'};
			
			vsotaA = sum(abs(najdeni2(1,:)))
			
			plot([najdeni1(2,1),najdeni1(2,end)],[0,0],'g')
		end
		legend(handles(:), legends);
	
		%legend('original file','andrej1.s2 janez metoda','andrej1.s2 andrej metoda',
		%'andrej2.s2 janez metoda','andrej2.s2 andrej metoda',
		%'primerjava janez metoda','primerjava andrej metoda')
		xlabel('time [ns]')
		%legend
		%riši nad prvih nekaj paketi
	else
		%plot(a:b,svolt1(a:b),'-r')  
		plot(a:b,svolt2(a:b),'-g')
		plot(a:b,svolt4(a:b),'-m')
		 
		 
		 
		plot(tpeaks2,svolt2(tpeaks2),'g*')
		plot(tpeaks4,svolt4(tpeaks4),'m*')
		 
		xlabel('paketki')
	end
end


%%%%%%  COUNTER in TIME
%%razen lokalno neuporabno
if 0
  figure;
  hold on; 
  plot(ptime1,pcounter1,'r.','markers',20)
  plot(ptime2,pcounter2,'g.','markers',14)
end

if 0
  figure;
  hold on; 
  plot(pcounter1./14,ptime1,'r.','markers',20)
  plot(pcounter2./14,ptime2,'g.','markers',14)
  plot(pcounter3./14,ptime3,'b.','markers',8)
  xlabel('counter')
  ylabel('time [ns]')
end

%%%%   PING - razlika timestampov od paketa do paketa (hitrost)
if 0
  figure;
  hold on;
  %plot(1:size(ping1),ping1,'r')
  plot(1:size(ping2),ping2,'g')
  plot(1:size(ping3),ping3,'b')
end


%%%%    JITTER - razlika ping med tremi paketi (pospešek)
if 0
  figure;
  hold on;
  %plot(1:size(jitter1),jitter1,'r')
  plot(ptime2(2:end-1),jitter2,'g')
  %plot(,jitter2>10)
  plot(ptime3(2:end-1),jitter3,'b')
  xlabel("counter")
  ylabel("jitter [ns]")
end






%%%%%%%%%%%%%%       IZPIS statiske simpleS         %%%%%%%%%%%%%
if 0
  "izpis simple"
  prviPaketTime = ptime2(1)
  zadnjiPaketTime = ptime2(end)
  slabih = sum(ping2 > (2*10^8))
  normalnih = sum(ping2>(10^8))
  paketov = size(ping2)(1)
  razmerje = normalnih/paketov
  lomi = sum(abs(jitter2)>15)
end



%%%%%%%%%%%%%%       IZPIS statiske S         %%%%%%%%%%%%%
if 0
  "izpis signal"
  prviPaketTime = ptime3(1)
  zadnjiPaketTime = ptime3(end)
  slabih = sum(ping3 > (2*10^8))
  normalnih = sum(ping3>(10^8))
  paketov = size(ping3)(1)
  razmerje = normalnih/paketov
  lomi = sum(abs(jitter3)>15)
end




%%%%%%%%%%%%%%       izpis statiske PRIMERJAVE        %%%%%%%%%%%%%

if 0
    "izpis primerjava"
    simplePeaks = size(speaks2)(1)
    signalPeaks = size(speaks4)(1)
    if (size(speaks2) == size(speaks4))
        enakih = sum(speaks2 == speaks3)
    end
end




'DONE'